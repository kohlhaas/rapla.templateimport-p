/*--------------------------------------------------------------------------*
 | Copyright (C) 2006 Christopher Kohlhaas                                  |
 |                                                                          |
 | This program is free software; you can redistribute it and/or modify     |
 | it under the terms of the GNU General Public License as published by the |
 | Free Software Foundation. A copy of the license has been included with   |
 | these distribution in the COPYING file, if not go to www.fsf.org         |
 |                                                                          |
 | As a special exception, you are granted the permissions to link this     |
 | program with every library, which license fulfills the Open Source       |
 | Definition as published by the Open Source Initiative (OSI).             |
 *--------------------------------------------------------------------------*/
package org.rapla.plugin.demo;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;

import org.rapla.entities.domain.Reservation;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.TreeFactory;
import org.rapla.gui.toolkit.RaplaWidget;

/** sample UseCase that only displays the text of the configuration and
 all reservations of the user.*/
class MyDialog extends RaplaGUIComponent implements  RaplaWidget
{

    JLabel label = new JLabel();
    JTree tree;
    JPanel panel = new JPanel();
    public final static String ROLE = MyDialog.class.getName();

    public MyDialog(RaplaContext sm) throws RaplaException {
        super(sm);
        setChildBundleName( MyPlugin.RESOURCE_FILE);
        getLogger().info("MyUseCase started");
        Reservation[] reservations = getQuery().getReservations(getUser(),null,null,null);
        tree = new JTree(getTreeFactory().createClassifiableModel(reservations));
        tree.setCellRenderer(getTreeFactory().createRenderer());
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        panel.setBorder(BorderFactory.createEmptyBorder(40,40,40,40));
        panel.setLayout(new BorderLayout());
        panel.add(label,BorderLayout.NORTH);
        panel.add(tree,BorderLayout.CENTER);
        label.setText( getString("my_reservations"));
    }

    private TreeFactory getTreeFactory()
    {
        return  getService(TreeFactory.class);
    }

    public JComponent getComponent() {
        return panel;
    }
}

