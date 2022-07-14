package com.warmthdawn.kubejsdebugadapter.utils;

import com.warmthdawn.kubejsdebugadapter.adapter.DebuggerLauncher;
import net.minecraft.client.resources.language.I18n;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.concurrent.locks.ReentrantLock;

public class NotifyDialog {
    private static volatile boolean cancelWaiting = false;

    private static final ReentrantLock lock = new ReentrantLock();
    private static JFrame dialog;

    public static void showNotice(int port) {
        Thread thread = new Thread(() -> {
            lock.lock();
            try {
                dialog = createDialog(port, () -> cancelWaiting = true);
                dialog.setVisible(true);
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                lock.unlock();
            }
        });
        thread.start();
    }

    public static void close() {

        lock.lock();
        try {
            if (dialog != null) {
                dialog.setVisible(false);
                dialog.dispose();
                dialog = null;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public static boolean isCancelWaiting() {
        return cancelWaiting;
    }

    public static JFrame createDialog(int port, Runnable cancelCallback) {


        String msg1 = "Listening the debugger on port " + port;
        String msg2 = "Please start your vscode and attach the debugger to this.";
        String cancel = I18n.get("Cancel");
        String title = I18n.get("Waiting for the debugger");

        JFrame frame = new JFrame();
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int width = gd.getDisplayMode().getWidth() >= 1920 ? (int) (gd.getDisplayMode().getWidth() * 0.2) : 384;
        int height = (int) (width * 0.4);
        frame.setBounds((Toolkit.getDefaultToolkit().getScreenSize().width - width) / 2,
            (Toolkit.getDefaultToolkit().getScreenSize().height - height) / 25 * 10, width, height);
        frame.setTitle(title);
        JPanel contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        frame.setContentPane(contentPane);
        contentPane.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

        JLabel label1 = new JLabel(msg1);
        JLabel label2 = new JLabel(msg2);
        contentPane.add(label1);
        contentPane.add(label2);
        // 绘制进度条
        // 在下载未完成时禁止玩家关闭窗口
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        // 取消按钮
        JButton btCancel = new JButton(cancel) {
            @Override
            protected void fireActionPerformed(ActionEvent event) {
                super.fireActionPerformed(event);
                cancelCallback.run();
                frame.setVisible(false);
                frame.dispose();
            }
        };
        btCancel.setSize(width / 5, height / 5);
        btCancel.setLayout(new FlowLayout(FlowLayout.TRAILING, 5, 5));
        contentPane.add(btCancel);

        return frame;
    }

}
