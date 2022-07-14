package com.warmthdawn.kubejsdebugadapter;

import com.warmthdawn.kubejsdebugadapter.adapter.DebuggerLauncher;
import com.warmthdawn.kubejsdebugadapter.debugger.DebugRuntime;
import com.warmthdawn.kubejsdebugadapter.utils.NotifyDialog;
import dev.latvian.kubejs.KubeJSPlugin;
import net.minecraft.client.resources.language.I18n;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Executors;

public class DebuggerPlugin extends KubeJSPlugin {


    @Override
    public void init() {
        DebuggerLauncher.markKubeJSLoaded();

        try {
            Thread thread = new Thread(() -> {
                DebugRuntime runtime = DebugRuntime.getInstance();
                while (!(runtime.isConfigurationDone() || NotifyDialog.isCancelWaiting())) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });

            thread.start();
            thread.join();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        NotifyDialog.close();
    }


}
