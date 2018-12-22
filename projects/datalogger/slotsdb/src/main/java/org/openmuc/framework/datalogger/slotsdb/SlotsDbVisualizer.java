/*
 * Copyright 2011-18 Fraunhofer ISE
 *
 * This file is part of OpenMUC.
 * For more information visit http://www.openmuc.org
 *
 * OpenMUC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenMUC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenMUC.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.openmuc.framework.datalogger.slotsdb;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import org.openmuc.framework.data.Record;

/**
 * Class providing a graphical UI to view the content of a .opm file
 * 
 */
public final class SlotsDbVisualizer extends JFrame {

    private static final long serialVersionUID = 1L;
    JFileChooser fc = new JFileChooser();
    File file;
    String[][] rowData = { { "0", "0", "0" } };
    String[] columnNames = { "Time", "Value", "State" };
    JTable table = new JTable(rowData, columnNames);
    JScrollPane content = new JScrollPane(table);

    public SlotsDbVisualizer() {

        JTextField fileNameField = new JTextField(15);
        fileNameField.setEditable(false);

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem openItem = new JMenuItem("Open");
        openItem.addActionListener(new openFileListener());

        menuBar.add(fileMenu);
        fileMenu.add(openItem);

        setJMenuBar(menuBar);
        setContentPane(content);
        setTitle(SlotsDb.FILE_EXTENSION + " File Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);

    }

    class openFileListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            int ret = fc.showOpenDialog(SlotsDbVisualizer.this);
            if (ret == JFileChooser.APPROVE_OPTION) {
                file = fc.getSelectedFile();
                java.util.List<Record> res = null;
                try {
                    FileObject fo = new FileObject(file);
                    res = fo.readFully();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                if (res != null) {
                    String[][] tblData = new String[res.size()][3];
                    Calendar cal = Calendar.getInstance();
                    for (int i = 0; i < res.size(); i++) {
                        cal.setTimeInMillis(res.get(i).getTimestamp());

                        // tblData[i][0] = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(cal.getTime());
                        tblData[i][0] = res.get(i).getTimestamp().toString();
                        tblData[i][1] = Double.toString(res.get(i).getValue().asDouble());
                        tblData[i][2] = Integer.toString(res.get(i).getFlag().getCode());
                    }
                    table = new JTable(tblData, columnNames);
                    content = new JScrollPane(table);
                    setContentPane(content);
                    invalidate();
                    validate();
                }
            }
        }
    }

    public static void main(String[] args) {
        JFrame window = new SlotsDbVisualizer();
        window.setVisible(true);
    }
}
