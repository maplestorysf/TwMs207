/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.swing.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import server.swing.Progressbar;
import tools.EncodingDetect;

/**
 *
 * @author pungin
 */
public class FixJavaScript extends javax.swing.JFrame {

    /**
     * Creates new form FixJavaScript
     */
    public FixJavaScript() {
        initComponents();
    }

    @Override
    public void setVisible(boolean bln) {
        if (bln) {
            init();
            setLocationRelativeTo(null);
        }
        super.setVisible(bln);
    }

    public void init() {
        jTextField1.setText("");
        jTextField2.setText("js");
    }

    public void listDirectory(File path, List<File> myfile) {
        if (!path.exists()) {
            System.out.println("檔案不存在!");
        } else {
            if (path.isFile()) {
                if (path.getName().endsWith("." + jTextField2.getText().toLowerCase())
                        || path.getName().endsWith("." + jTextField2.getText().toUpperCase())) {
                    myfile.add(path);
                }
            } else {
                File[] files = path.listFiles();
                for (int i = 0; i < files.length; i++) {
                    listDirectory(files[i], myfile);
                }
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated
    // Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jButton2 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("修正JavaScript");

        jLabel1.setText("路徑");

        jButton1.setText("...");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jLabel2.setForeground(new java.awt.Color(255, 0, 0));
        jLabel2.setText("* 會包含子目錄會全部都處理");

        jLabel3.setText("檔案後綴");

        jTextField2.setText("js");

        jButton2.setText("開始");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout
                .createSequentialGroup().addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup().addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 40,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jButton2))
                        .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup().addComponent(jLabel1)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 271,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jButton1))
                                        .addComponent(jLabel2))
                                .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout
                .createSequentialGroup().addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel1)
                        .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jButton1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel3)
                        .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jButton2))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton1ActionPerformed
        JFileChooser fd = new JFileChooser();
        fd.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fd.showOpenDialog(null);
        File file = fd.getSelectedFile();
        if (file != null) {
            if (!file.exists() || !file.isDirectory()) {
                System.out.println("資料夾不存在");
                return;
            }
            jTextField1.setText(file.getPath());
        }
    }// GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton2ActionPerformed
        Thread th = new Thread() {
            @Override
            public void run() {
                File file = new File(jTextField1.getText());
                if (file.exists() && file.isDirectory()) {
                    List<File> myfile = new ArrayList<>();
                    listDirectory(file, myfile);
                    Progressbar.visible(true);
                    Progressbar.updateTitle("編碼轉換");
                    Progressbar.setMaximum(myfile.size());
                    for (File f : myfile) {
                        String fileStr = null;
                        String code = EncodingDetect.getJavaEncode(f);
                        boolean addNashorn = false;
                        try (Stream<String> stream = Files.lines(f.toPath(), Charset.forName(code))) {
                            String lines = stream.collect(Collectors.joining(System.lineSeparator())).toLowerCase();
                            if (!lines.contains("load('nashorn:mozilla_compat.js');")
                                    && (lines.contains("importpackage(") || lines.contains("importclass("))) {
                                addNashorn = true;
                            }
                        } catch (IOException ex) {
                        }
                        if (addNashorn || (f.exists() && !f.isDirectory() && !code.toUpperCase().equals("UTF-8")
                                && !code.toUpperCase().equals("ASCII"))) {
                            System.out.println(
                                    "檔案\"" + f.getName() + "\"編碼(" + code + ") 是否需要適配Java8(" + addNashorn + ") 被處理");
                            try {
                                BufferedReader bf = new BufferedReader(
                                        new InputStreamReader(new FileInputStream(f), code));
                                fileStr = (addNashorn ? "load('nashorn:mozilla_compat.js');\r\n" : "")
                                        + bf.lines().collect(Collectors.joining(System.lineSeparator()));
                                bf.close();
                            } catch (Exception ex) {
                            }
                        }
                        if (fileStr != null) {
                            FileOutputStream out = null;
                            try {
                                out = new FileOutputStream(f);
                                OutputStreamWriter osw = new OutputStreamWriter(out, "UTF-8");
                                osw.write(fileStr);
                                osw.flush();
                            } catch (IOException ess) {
                            } finally {
                                try {
                                    if (out != null) {
                                        out.close();
                                    }
                                } catch (IOException ignore) {
                                }
                            }
                        }
                        Progressbar.addValue();
                    }
                    JOptionPane.showMessageDialog(null, "處理完成。");
                    Progressbar.visible(false);
                }
            }
        };
        th.start();
    }// GEN-LAST:event_jButton2ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    // End of variables declaration//GEN-END:variables
}
