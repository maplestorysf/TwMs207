/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.swing.tools;

import handling.RecvPacketOpcode;
import handling.SendPacketOpcode;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import tools.HexTool;

/**
 *
 * @author Pungin
 */
public class ConvertOpcodes extends javax.swing.JFrame {

    /**
     * Creates new form ConvertOpcodes
     */
    public ConvertOpcodes() {
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
        jRadioButton1.setSelected(true);
        jRadioButton3.setSelected(true);
        jTextField1.setText("recvops");
        jTextField2.setText("sendops");
    }

    public static void convertOpcodes(String[] args) {
        boolean decimal;
        boolean positive;
        String recvopsName;
        String sendopsName;
        Scanner input = new Scanner(System.in);
        if (args != null) {
            try {
                decimal = Boolean.parseBoolean(args[0]);
            } catch (Exception e) {
                decimal = false;
            }
            try {
                positive = Boolean.parseBoolean(args[1]);
            } catch (Exception e) {
                positive = false;
            }
            try {
                recvopsName = args[2] + ".properties";
            } catch (Exception e) {
                recvopsName = "recvops.properties";
            }
            try {
                sendopsName = args[3] + ".properties";
            } catch (Exception e) {
                sendopsName = "sendops.properties";
            }
        } else {
            System.out.println("歡迎使用包頭轉換器 \r\n你可以選擇十六進制或者十進制的包頭值, \r\n然後它們會被保存到新的檔案中");
            // RecvPacketOpcode.reloadValues();
            // SendPacketOpcode.reloadValues();
            System.out.println("你想轉換成多少進制? 16 還是 10?");
            decimal = "10".equals(input.next().toLowerCase());
            System.out.println("輸出檔案為正序請出入1,其他則為倒序");
            positive = "1".equals(input.next().toLowerCase());
            System.out.println("\r\n輸入你要儲存的用戶端包頭值檔案名字(輸入1為recvops): \r\n");
            recvopsName = input.next();
            if (recvopsName.equals("1")) {
                recvopsName = "recvops.properties";
            } else {
                recvopsName += ".properties";
            }
            System.out.println("\r\n輸入你要儲存的伺服端包頭值檔案名字(輸入1為sendops): \r\n");
            sendopsName = input.next();
            if (sendopsName.equals("1")) {
                sendopsName = "sendops.properties";
            } else {
                sendopsName += ".properties";
            }
        }
        StringBuilder sb = new StringBuilder();
        FileOutputStream out;
        try {
            RecvPacketOpcode.loadValues();
            out = new FileOutputStream(recvopsName, false);
            for (RecvPacketOpcode recv : RecvPacketOpcode.values()) {
                if (recv == RecvPacketOpcode.UNKNOWN) {
                    break;
                }
                if (positive) {
                    sb.append(recv.name()).append(" = ")
                            .append(decimal ? recv.getValue() : HexTool.getOpcodeToString(recv.getValue()))
                            .append("\r\n");
                } else {
                    sb.insert(0, "\r\n")
                            .insert(0, decimal ? recv.getValue() : HexTool.getOpcodeToString(recv.getValue()))
                            .insert(0, " = ").insert(0, recv.name());
                }
            }
            out.write(sb.toString().getBytes());
            out.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ConvertOpcodes.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ConvertOpcodes.class.getName()).log(Level.SEVERE, null, ex);
        }
        sb = new StringBuilder();
        try {
            SendPacketOpcode.loadValues();
            out = new FileOutputStream(sendopsName, false);
            for (SendPacketOpcode send : SendPacketOpcode.values()) {
                if (send == SendPacketOpcode.UNKNOWN) {
                    break;
                }
                if (positive) {
                    sb.append(send.name()).append(" = ")
                            .append(decimal ? send.getValue(false) : HexTool.getOpcodeToString(send.getValue(false)))
                            .append("\r\n");
                } else {
                    sb.insert(0, "\r\n")
                            .insert(0, decimal ? send.getValue(false) : HexTool.getOpcodeToString(send.getValue(false)))
                            .insert(0, " = ").insert(0, send.name());
                }
            }
            out.write(sb.toString().getBytes());
            out.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ConvertOpcodes.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ConvertOpcodes.class.getName()).log(Level.SEVERE, null, ex);
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

        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();
        jRadioButton1 = new javax.swing.JRadioButton();
        jRadioButton2 = new javax.swing.JRadioButton();
        jRadioButton3 = new javax.swing.JRadioButton();
        jRadioButton4 = new javax.swing.JRadioButton();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("包頭轉換器");
        setResizable(false);

        buttonGroup1.add(jRadioButton1);
        jRadioButton1.setSelected(true);
        jRadioButton1.setText("16進制");

        buttonGroup1.add(jRadioButton2);
        jRadioButton2.setText("10進制");

        buttonGroup2.add(jRadioButton3);
        jRadioButton3.setSelected(true);
        jRadioButton3.setText("正序");

        buttonGroup2.add(jRadioButton4);
        jRadioButton4.setText("倒序");

        jLabel2.setText("輸出進制:");

        jLabel3.setText("輸出順序:");

        jTextField1.setText("recvops");

        jLabel4.setText("用戶端檔案名:");

        jLabel5.setText("伺服端檔案名:");

        jTextField2.setText("sendops");

        jButton1.setText("轉換");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout
                .createSequentialGroup().addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 179,
                                javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup().addComponent(jLabel2)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(jRadioButton1))
                                        .addGroup(layout.createSequentialGroup().addComponent(jLabel3)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(jRadioButton3)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jRadioButton4).addComponent(jRadioButton2)))
                        .addGroup(layout.createSequentialGroup().addComponent(jLabel4)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 100,
                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(layout.createSequentialGroup().addComponent(jLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 100,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup().addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel2).addComponent(jRadioButton1).addComponent(jRadioButton2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel3).addComponent(jRadioButton3).addComponent(jRadioButton4))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel4))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel5).addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jButton1)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton1ActionPerformed
        convertOpcodes(new String[]{String.valueOf(jRadioButton2.isSelected()),
            String.valueOf(jRadioButton3.isSelected()), jTextField1.getText(), jTextField2.getText()});
    }// GEN-LAST:event_jButton1ActionPerformed
     // Variables declaration - do not modify//GEN-BEGIN:variables

    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JRadioButton jRadioButton1;
    private javax.swing.JRadioButton jRadioButton2;
    private javax.swing.JRadioButton jRadioButton3;
    private javax.swing.JRadioButton jRadioButton4;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    // End of variables declaration//GEN-END:variables
}
