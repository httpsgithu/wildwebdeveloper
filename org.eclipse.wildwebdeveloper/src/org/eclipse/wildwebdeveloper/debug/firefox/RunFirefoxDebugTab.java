/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Mickael Istria (Red Hat Inc.) - initial implementation
 *******************************************************************************/
package org.eclipse.wildwebdeveloper.debug.firefox;

import static org.eclipse.wildwebdeveloper.debug.SelectionUtils.getSelectedFile;
import static org.eclipse.wildwebdeveloper.debug.SelectionUtils.getSelectedProject;
import static org.eclipse.wildwebdeveloper.debug.SelectionUtils.pathOrEmpty;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wildwebdeveloper.Activator;
import org.eclipse.wildwebdeveloper.debug.AbstractDebugAdapterLaunchShortcut;
import org.eclipse.wildwebdeveloper.debug.Messages;
import org.eclipse.wildwebdeveloper.debug.node.NodeRunDAPDebugDelegate;

public class RunFirefoxDebugTab extends AbstractLaunchConfigurationTab {

	private Text programPathText;
	private Text argumentsText;
	private Text workingDirectoryText;
	private Button reloadOnChange;
	private final AbstractDebugAdapterLaunchShortcut shortcut = new FirefoxRunDebugLaunchShortcut(); // contains many utilities

	@Override
	public void createControl(Composite parent) {
		Composite resComposite = new Composite(parent, SWT.NONE);
		resComposite.setLayout(new GridLayout(3, false));
		new Label(resComposite, SWT.NONE).setText(Messages.FirefoxDebugTab_File);
		this.programPathText = new Text(resComposite, SWT.BORDER);
		this.programPathText.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false));
		ControlDecoration decoration = new ControlDecoration(programPathText, SWT.TOP | SWT.LEFT);
		FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR);
		decoration.setImage(fieldDecoration.getImage());
		this.programPathText.addModifyListener(event -> {
			setDirty(true);
			File file = new File(programPathText.getText());
			if (!file.isFile()) {
				String errorMessage = Messages.RunProgramTab_error_unknownFile;
				setErrorMessage(errorMessage);
				decoration.setDescriptionText(errorMessage);
				decoration.show();
			} else if (!shortcut.canLaunch(file)) {
				String errorMessage = "Not a html file";
				setErrorMessage(errorMessage);
				decoration.setDescriptionText(errorMessage);
				decoration.show();
			}
			else if (!file.canRead()) {
				String errorMessage = Messages.RunProgramTab_error_nonReadableFile;
				setErrorMessage(errorMessage);
				decoration.setDescriptionText(errorMessage);
				decoration.show();
			} else {
				setErrorMessage(null);
				decoration.hide();
			}
			updateLaunchConfigurationDialog();
		});
		Button filePath = new Button( resComposite, SWT.PUSH);
		filePath.setText("Browse...");
		filePath.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog filePathDialog = new FileDialog(resComposite.getShell());
				filePathDialog.setFilterPath(workingDirectoryText.getText());
				filePathDialog.setText("Select a .html file to debug");
				String path = filePathDialog.open();
				if (path != null) {
					programPathText.setText(path);
					setDirty(true);
					updateLaunchConfigurationDialog();
				}
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				this.widgetSelected(e);
			}
		});

		new Label(resComposite, SWT.NONE).setText(Messages.RunProgramTab_argument);
		this.argumentsText = new Text(resComposite, SWT.BORDER);
		GridData argsGD = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
		argsGD.horizontalSpan = 2;
		this.argumentsText.setLayoutData(argsGD);
		this.argumentsText.addModifyListener(e -> {
			setDirty(true);
			updateLaunchConfigurationDialog();
		});
		new Label(resComposite, SWT.NONE).setText(Messages.RunProgramTab_workingDirectory);
		this.workingDirectoryText = new Text(resComposite, SWT.BORDER);
		this.workingDirectoryText.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false));
		this.workingDirectoryText.addModifyListener(e -> {
			setDirty(true);
			updateLaunchConfigurationDialog();
		});
		Button workingDirectoryButton = new Button( resComposite, SWT.PUSH);
		workingDirectoryButton.setText("Browse...");
		workingDirectoryButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog workingDirectoryDialog = new DirectoryDialog(resComposite.getShell());
				workingDirectoryDialog.setFilterPath(workingDirectoryText.getText());
				workingDirectoryDialog.setText("Select folder to watch for changes");
				String path = workingDirectoryDialog.open();
				if (path != null) {
					workingDirectoryText.setText(path);
					setDirty(true);
					updateLaunchConfigurationDialog();
				}
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				this.widgetSelected(e);
				
			}
		});
		
		reloadOnChange = new Button(resComposite, SWT.CHECK);
		reloadOnChange.setText("Reload on change: ");
		reloadOnChange.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				setDirty(true);
				updateLaunchConfigurationDialog();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				this.widgetSelected(e);
			}
		});
		setControl(resComposite);
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		// Nothing to do
		}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			String defaultSelectedFile = pathOrEmpty(getSelectedFile(shortcut::canLaunch));
			this.programPathText.setText(configuration.getAttribute(FirefoxRunDABDebugDelegate.FILE, defaultSelectedFile)); // $NON-NLS-1$
			this.argumentsText.setText(configuration.getAttribute(NodeRunDAPDebugDelegate.ARGUMENTS, "")); //$NON-NLS-1$
			this.workingDirectoryText.setText(configuration.getAttribute(FirefoxRunDABDebugDelegate.WORKING_DIRECTORY, pathOrEmpty(getSelectedProject()))); //$NON-NLS-1$
		} catch (CoreException e) {
			Activator.getDefault().getLog().log(e.getStatus());
		}
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(FirefoxRunDABDebugDelegate.FILE, this.programPathText.getText());
		configuration.setAttribute(NodeRunDAPDebugDelegate.ARGUMENTS, this.argumentsText.getText());
		configuration.setAttribute(FirefoxRunDABDebugDelegate.WORKING_DIRECTORY, this.workingDirectoryText.getText());
		configuration.setAttribute(FirefoxRunDABDebugDelegate.RELOAD_ON_CHANGE, reloadOnChange.getSelection());
	}

	@Override
	public String getName() {
		return Messages.RunProgramTab_title;
	}

}