package com.warmthdawn.kubejsdebugadapter.adapter;

import com.warmthdawn.kubejsdebugadapter.data.breakpoint.ScriptBreakpointInfo;
import com.warmthdawn.kubejsdebugadapter.data.breakpoint.ScriptSourceData;
import com.warmthdawn.kubejsdebugadapter.data.variable.LazyVariable;
import com.warmthdawn.kubejsdebugadapter.debugger.DebugRuntime;
import com.warmthdawn.kubejsdebugadapter.debugger.DebugSession;
import com.warmthdawn.kubejsdebugadapter.debugger.DebugThread;
import com.warmthdawn.kubejsdebugadapter.debugger.KubeStackFrame;
import com.warmthdawn.kubejsdebugadapter.utils.*;
import com.warmthdawn.kubejsdebugadapter.data.variable.IVariableTreeNode;
import com.warmthdawn.kubejsdebugadapter.data.variable.VariableScope;
import dev.latvian.mods.rhino.ContextFactory;
import dev.latvian.mods.rhino.Scriptable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.debug.*;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KubeDebugAdapter implements IDebugProtocolServer {
    private static final Logger log = LogManager.getLogger();


    private final ExecutorService async;
    private final ExecutorService compilerAsync;

    public KubeDebugAdapter() {
        async = Executors.newSingleThreadExecutor();
        compilerAsync = Executors.newSingleThreadExecutor();
    }

    private DebuggerBridge bridge;
    private DebugRuntime runtime;

    private final DebugSession session = new DebugSession();


    private IDebugProtocolClient client;
    private DataConverter converter;

    @Override
    public CompletableFuture<Capabilities> initialize(InitializeRequestArguments args) {

        this.converter = new DataConverter(args.getLinesStartAt1(), args.getColumnsStartAt1());
        this.bridge = new DebuggerBridge(this.client, converter);
        this.runtime.setBridge(bridge);
        this.bridge.sendOutput("Connected to debug adapter");
        log.info("Connected to debug adapter");


        Capabilities capabilities = new Capabilities();

        capabilities.setSupportSuspendDebuggee(true);
        capabilities.setSupportsCompletionsRequest(true);
        capabilities.setSupportsEvaluateForHovers(true);
        capabilities.setSupportsConfigurationDoneRequest(true);
        capabilities.setSupportsBreakpointLocationsRequest(true);
        capabilities.setCompletionTriggerCharacters(new String[]{".", "[", "'", "\""});


        capabilities.setSupportsTerminateRequest(false);
        capabilities.setSupportTerminateDebuggee(false);
        capabilities.setSupportsTerminateThreadsRequest(false);
        capabilities.setSupportsInstructionBreakpoints(false);

        DebuggerLauncher.runOnLoaded(() -> {
            this.client.initialized();
            this.bridge.sendOutput("Debugger initialized");
        });

        return CompletableFuture.completedFuture(capabilities);
    }


    @Override
    public CompletableFuture<Void> attach(Map<String, Object> args) {
        return new CompletableFuture<>();
    }

    public void connect(IDebugProtocolClient client) {
        this.client = client;


        this.runtime = DebugRuntime.getInstance();

    }




    @Override
    public CompletableFuture<Void> configurationDone(ConfigurationDoneArguments args) {
        return CompletableFuture.runAsync(() -> {
            this.runtime.setConfigurationDone();
        }, async);
    }

    @Override
    public CompletableFuture<Void> disconnect(DisconnectArguments args) {
        return CompletableFuture.runAsync(() -> {
            this.runtime.resumeAll();
            this.runtime.removeBridge();
        }, async);
    }

    @Override
    public CompletableFuture<BreakpointLocationsResponse> breakpointLocations(BreakpointLocationsArguments args) {
        return CompletableFuture.supplyAsync(() -> {
            Source source = args.getSource();
            String sourceId = PathUtil.getSourceId(source);
            ScriptSourceData sourceData = runtime.getSourceManager().getSourceData(sourceId);
            if(sourceData == null) {
                sourceData = runtime.getSourceManager().compileSource(sourceId);
            }
            if(!sourceData.isFinished()) {
                Utils.waitFor(sourceData::isFinished);
            }
            int startLine = converter.toKubeLineNumber(args.getLine());
            int endLine = args.getEndLine() == null ? startLine : converter.toKubeLineNumber(args.getEndLine());
            int startColumn = args.getColumn() == null ? 0 : converter.toKubeColumnNumber(args.getColumn());
            int endColumn = args.getEndColumn() == null ? -1 : converter.toKubeColumnNumber(args.getEndColumn());
            List<ScriptBreakpointInfo> locationList = sourceData.getLocationList(startLine, endLine, startColumn, endColumn);

            BreakpointLocation[] breakpointLocations = converter.toDAPBreakpointLocations(locationList);

            BreakpointLocationsResponse resp = new BreakpointLocationsResponse();
            resp.setBreakpoints(breakpointLocations);
            return resp;

        }, compilerAsync);
    }

    @Override
    public CompletableFuture<SetBreakpointsResponse> setBreakpoints(SetBreakpointsArguments args) {
        return CompletableFuture.supplyAsync(() -> {
            List<Breakpoint> breakpoints = bridge.getBreakpointManager().setBreakpoints(args, this.converter);
            SetBreakpointsResponse response = new SetBreakpointsResponse();
            response.setBreakpoints(breakpoints.toArray(new Breakpoint[0]));
            return response;
        }, async);
    }

    @Override
    public CompletableFuture<SetFunctionBreakpointsResponse> setFunctionBreakpoints(SetFunctionBreakpointsArguments args) {
        return CompletableFuture.completedFuture(new SetFunctionBreakpointsResponse());
    }

    @Override
    public CompletableFuture<SetExceptionBreakpointsResponse> setExceptionBreakpoints(SetExceptionBreakpointsArguments args) {
        return CompletableFuture.completedFuture(new SetExceptionBreakpointsResponse());
    }

    @Override
    public CompletableFuture<DataBreakpointInfoResponse> dataBreakpointInfo(DataBreakpointInfoArguments args) {
        return CompletableFuture.completedFuture(new DataBreakpointInfoResponse());
    }

    @Override
    public CompletableFuture<SetDataBreakpointsResponse> setDataBreakpoints(SetDataBreakpointsArguments args) {
        return CompletableFuture.completedFuture(new SetDataBreakpointsResponse());
    }

    @Override
    public CompletableFuture<StackTraceResponse> stackTrace(StackTraceArguments args) {
        return CompletableFuture.supplyAsync(() -> {
            int threadId = args.getThreadId();
            DebugThread thread = runtime.getThread(threadId);
            StackTraceResponse response = new StackTraceResponse();
            response.setStackFrames(converter.toDAPStackFrames(thread.stackFrames(), session));
            return response;
        }, async);
    }

    @Override
    public CompletableFuture<ScopesResponse> scopes(ScopesArguments args) {
        return CompletableFuture.supplyAsync(() -> {
            int frameId = args.getFrameId();
            KubeStackFrame stackFrame = session.getStackFrame(frameId);
            List<VariableScope> frameVariables = session.getFrameVariables(stackFrame);
            ScopesResponse response = new ScopesResponse();
            response.setScopes(converter.toDAPScopes(frameVariables));
            return response;
        }, async);
    }

    @Override
    public CompletableFuture<VariablesResponse> variables(VariablesArguments args) {
        return CompletableFuture.supplyAsync(() -> {
            int id = args.getVariablesReference();
            IVariableTreeNode variable = session.getVariable(id);
            VariablesResponse response = new VariablesResponse();
            List<IVariableTreeNode> children = variable.getChildren(session);

            Variable[] variables = new Variable[children.size()];
            for (int i = 0; i < children.size(); i++) {
                variables[i] = converter.toDAPVariable(children.get(i));
            }
            response.setVariables(variables);
            return response;
        }, compilerAsync);
    }

    @Override
    public CompletableFuture<ThreadsResponse> threads() {
        return CompletableFuture.supplyAsync(() -> {
            ThreadsResponse response = new ThreadsResponse();
            response.setThreads(converter.toDAPThread(runtime.getThreads()));
            return response;
        }, async);
    }


    @Override
    public CompletableFuture<ContinueResponse> continue_(ContinueArguments args) {
        return CompletableFuture.supplyAsync(() -> {
            boolean resumeAll = args.getSingleThread() == null || !args.getSingleThread();
            if (resumeAll) {
                runtime.resumeAll();
            } else {
                int threadId = args.getThreadId();
                DebugThread thread = runtime.getThread(threadId);
                thread.resume();
            }
            session.clearPool();
            ContinueResponse response = new ContinueResponse();
            response.setAllThreadsContinued(resumeAll);
            return response;
        }, async);
    }

    @Override
    public CompletableFuture<Void> next(NextArguments args) {
        return CompletableFuture.runAsync(() -> {
            int threadId = args.getThreadId();
            DebugThread thread = runtime.getThread(threadId);

            thread.stepOver();

        }, async);
    }

    @Override
    public CompletableFuture<Void> stepIn(StepInArguments args) {
        return CompletableFuture.runAsync(() -> {
            int threadId = args.getThreadId();
            DebugThread thread = runtime.getThread(threadId);
            thread.stepInto();
        }, async);
    }

    @Override
    public CompletableFuture<Void> stepOut(StepOutArguments args) {
        return CompletableFuture.runAsync(() -> {
            int threadId = args.getThreadId();
            DebugThread thread = runtime.getThread(threadId);
            thread.stepOut();
        }, async);
    }

    @Override
    public CompletableFuture<Void> stepBack(StepBackArguments args) {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Void> goto_(GotoArguments args) {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Void> pause(PauseArguments args) {
        return CompletableFuture.runAsync(() -> {
            int threadId = args.getThreadId();
            DebugThread thread = runtime.getThread(threadId);
            thread.pause();
        }, async);
    }

    @Override
    public CompletableFuture<EvaluateResponse> evaluate(EvaluateArguments args) {
        return CompletableFuture.supplyAsync(() -> {
            int frameId = args.getFrameId();
            KubeStackFrame stackFrame = session.getStackFrame(frameId);
            String expression = args.getExpression();

            ContextFactory factory = stackFrame.getFactory();
            Scriptable scope = stackFrame.getScope();
            EvaluateResponse response = new EvaluateResponse();
            try {
                Object result = EvalUtils.evaluate(factory, expression, scope);
                IVariableTreeNode variable = session.createVariable(result, expression, factory);
                String resultString = VariableUtils.variableToString(factory, result);
                response.setResult(resultString);
                response.setVariablesReference(variable.getId());
            } catch (Throwable t) {
                response.setResult("Could not evalulate: " + expression);
                IVariableTreeNode variable = session.createError(t, expression, factory);
                response.setVariablesReference(variable.getId());
            }
            return response;
        }, compilerAsync);
    }

    @Override
    public CompletableFuture<CompletionsResponse> completions(CompletionsArguments args) {
        return CompletableFuture.supplyAsync(() -> {
            int frameId = args.getFrameId();
            KubeStackFrame stackFrame = session.getStackFrame(frameId);
            String expression = args.getText();

            List<CompletionItem> complete = CompletionUtils.complete(expression, stackFrame);


            CompletionsResponse response = new CompletionsResponse();
            response.setTargets(complete.toArray(new CompletionItem[0]));

            return response;
        }, compilerAsync);
    }


}
