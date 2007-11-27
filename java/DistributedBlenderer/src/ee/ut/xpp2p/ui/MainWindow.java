package ee.ut.xpp2p.ui;

import java.awt.Button;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import ee.ut.xpp2p.blenderer.MasterBlenderer;
import ee.ut.xpp2p.model.RenderJob;

/**
 * Main window of the program, made with Eclipse Visual Editor
 * 
 * @author Jaan Neljandik
 * @created 27.10.2007
 */
public class MainWindow extends JFrame {
	private static final long serialVersionUID = 1L;
	private JFrame mainWindow = null;
	private JPanel mainContentPane = null; // @jve:decl-index=0:visual-constraint="10,54"
	public JTextField inputFileTextField = null;
	public JTextField outputLocTextField = null;
	public JLabel inputFileErrorLabel = null;
	private JLabel inputFileLabel = null;
	private JLabel outputLocLabel = null;
	public JLabel outputLocErrorLabel = null;
	private Button inputFileButton = null;
	private Button outputLocButton = null;
	private JLabel filetypeLabel = null;
	public Choice filetypeChoice = null;
	private JLabel framesLabel = null;
	private JLabel startFrameLabel = null;
	private JLabel endFrameLabel = null;
	public JLabel framesErrorLabel = null;
	public JTextField startFrameTextField = null;
	public JTextField endFrameTextField = null;
	private Button exitButton = null;
	public Button renderButton = null;
	public InputValidator inputChecker = new InputValidator();
	private MasterBlenderer master;
	private JProgressBar jProgressBar = null;
	long frameCount = 0L;

	/**
	 * Initializes main window
	 */
	public MainWindow(MasterBlenderer master) {
		this.master = master;
		mainWindow = new JFrame("Specify parameters");
		mainWindow.setSize(new Dimension(430, 300));
		mainWindow.setContentPane(getMainContentPane());
		mainWindow.setLocationRelativeTo(null);
		mainWindow.setVisible(true);
	}

	/**
	 * This method initializes mainContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getMainContentPane() {
		if (mainContentPane == null) {
			framesErrorLabel = new JLabel();
			framesErrorLabel.setBounds(new Rectangle(26, 178, 259, 15));
			framesErrorLabel.setForeground(Color.red);
			framesErrorLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
			framesErrorLabel.setText("");
			endFrameLabel = new JLabel();
			endFrameLabel.setBounds(new Rectangle(164, 156, 41, 20));
			endFrameLabel.setText("End at: ");
			startFrameLabel = new JLabel();
			startFrameLabel.setBounds(new Rectangle(15, 156, 64, 20));
			startFrameLabel.setText("Start from:");
			framesLabel = new JLabel();
			framesLabel.setBounds(new Rectangle(16, 130, 166, 19));
			framesLabel.setText("Select start and end frames:");
			filetypeLabel = new JLabel();
			filetypeLabel.setBounds(new Rectangle(15, 91, 143, 20));
			filetypeLabel.setText("Select output file format:");
			outputLocErrorLabel = new JLabel();
			outputLocErrorLabel.setBounds(new Rectangle(26, 74, 259, 15));
			outputLocErrorLabel.setForeground(Color.red);
			outputLocErrorLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
			outputLocErrorLabel.setText("");
			outputLocLabel = new JLabel();
			outputLocLabel.setBounds(new Rectangle(15, 54, 143, 18));
			outputLocLabel.setText("Select ouput location:");
			inputFileLabel = new JLabel();
			inputFileLabel.setDisplayedMnemonic(KeyEvent.VK_UNDEFINED);
			inputFileLabel.setBounds(new Rectangle(15, 15, 143, 20));
			inputFileLabel.setText("Select input file:");
			inputFileErrorLabel = new JLabel();
			inputFileErrorLabel.setBounds(new Rectangle(26, 37, 259, 15));
			inputFileErrorLabel.setForeground(Color.red);
			inputFileErrorLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
			inputFileErrorLabel.setText("");
			mainContentPane = new JPanel();
			mainContentPane.setLayout(null);
			mainContentPane.setSize(new Dimension(430, 269));
			mainContentPane.add(getInputFileTextField(), null);
			mainContentPane.add(getOutputLocTextField(), null);
			mainContentPane.add(inputFileErrorLabel, null);
			mainContentPane.add(inputFileLabel, null);
			mainContentPane.add(outputLocLabel, null);
			mainContentPane.add(outputLocErrorLabel, null);
			mainContentPane.add(getInputFileButton(), null);
			mainContentPane.add(getOutputLocButton(), null);
			mainContentPane.add(filetypeLabel, null);
			mainContentPane.add(getFiletypeChoice(), null);
			mainContentPane.add(framesLabel, null);
			mainContentPane.add(startFrameLabel, null);
			mainContentPane.add(endFrameLabel, null);
			mainContentPane.add(framesErrorLabel, null);
			mainContentPane.add(getStartFrameTextField(), null);
			mainContentPane.add(getEndFrameTextField(), null);
			mainContentPane.add(getExitButton(), null);
			mainContentPane.add(getRenderButton(), null);
			mainContentPane.add(getJProgressBar(), null);
		}
		return mainContentPane;
	}

	/**
	 * This method initializes inputFileTextField
	 * 
	 * @return javax.swing.JTextField
	 */
	private JTextField getInputFileTextField() {
		if (inputFileTextField == null) {
			inputFileTextField = new JTextField();
			inputFileTextField.setBounds(new Rectangle(165, 15, 165, 20));
		}
		return inputFileTextField;
	}

	/**
	 * This method initializes outputLocTextField
	 * 
	 * @return javax.swing.JTextField
	 */
	private JTextField getOutputLocTextField() {
		if (outputLocTextField == null) {
			outputLocTextField = new JTextField();
			outputLocTextField.setBounds(new Rectangle(164, 54, 165, 20));
		}
		return outputLocTextField;
	}

	/**
	 * This method initializes inputFileButton
	 * 
	 * @return java.awt.Button
	 */
	private Button getInputFileButton() {
		if (inputFileButton == null) {
			inputFileButton = new Button();
			inputFileButton.setBounds(new Rectangle(337, 15, 80, 20));
			inputFileButton.setLabel("Browse");
			inputFileButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					inputFileButtonPressed();
				}
			});
		}
		return inputFileButton;
	}

	/**
	 * This method initializes outputLocButton
	 * 
	 * @return java.awt.Button
	 */
	private Button getOutputLocButton() {
		if (outputLocButton == null) {
			outputLocButton = new Button();
			outputLocButton.setBounds(new Rectangle(336, 54, 79, 21));
			outputLocButton.setLabel("Browse");
			outputLocButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					outputLocButtonPressed();
				}
			});
		}
		return outputLocButton;
	}

	/**
	 * This method initializes filetypeChoice
	 * 
	 * @return java.awt.Choice
	 */
	private Choice getFiletypeChoice() {
		if (filetypeChoice == null) {
			filetypeChoice = new Choice();
			filetypeChoice.setBounds(new Rectangle(164, 91, 138, 20));
			initFileTypeChoice();
		}
		return filetypeChoice;
	}

	/**
	 * Initializes File type selection
	 */
	private void initFileTypeChoice() {
		filetypeChoice.add("AVIJPEG");
		filetypeChoice.add("TGA");
		filetypeChoice.add("IRIS");
		filetypeChoice.add("HAMK");
		filetypeChoice.add("FTYPE");
		filetypeChoice.add("JPEG");
		filetypeChoice.add("MOVIE");
		filetypeChoice.add("IRIZ");
		filetypeChoice.add("RAWTGA");
		filetypeChoice.add("AVIRAW");
		filetypeChoice.add("PNG");
		filetypeChoice.add("BMP");
		filetypeChoice.add("FRAMESERVER");
	}

	/**
	 * This method initializes startFrameTextField
	 * 
	 * @return javax.swing.JTextField
	 */
	private JTextField getStartFrameTextField() {
		if (startFrameTextField == null) {
			startFrameTextField = new JTextField();
			startFrameTextField.setBounds(new Rectangle(85, 156, 73, 20));
			startFrameTextField.setText("1");
		}
		return startFrameTextField;
	}

	/**
	 * This method initializes endFrameTextField
	 * 
	 * @return javax.swing.JTextField
	 */
	private JTextField getEndFrameTextField() {
		if (endFrameTextField == null) {
			endFrameTextField = new JTextField();
			endFrameTextField.setBounds(new Rectangle(211, 156, 73, 20));
			endFrameTextField.setText("1");
		}
		return endFrameTextField;
	}

	/**
	 * This method initializes exitButton
	 * 
	 * @return java.awt.Button
	 */
	private Button getExitButton() {
		if (exitButton == null) {
			exitButton = new Button();
			exitButton.setBounds(new Rectangle(86, 231, 111, 23));
			exitButton.setLabel("Quit Program");
			exitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					quitButtonPressed();
				}
			});
		}
		return exitButton;
	}

	/**
	 * This method initializes renderButton
	 * 
	 * @return java.awt.Button
	 */
	private Button getRenderButton() {
		if (renderButton == null) {
			renderButton = new Button();
			renderButton.setBounds(new Rectangle(220, 231, 111, 23));
			renderButton.setLabel("Start Rendering");
			renderButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					renderButtonPressed();
				}
			});
		}
		return renderButton;
	}

	private void renderButtonPressed() {
		if (inputChecker.validate(this)) {
			RenderJob job = new RenderJob();
			job.setInputFile(inputFileTextField.getText());
			job.setOutputLocation(outputLocTextField.getText());
			job.setOutputFormat(filetypeChoice.getSelectedItem());
			job.setStartFrame(Long.parseLong(startFrameTextField.getText()));
			job.setEndFrame(Long.parseLong(endFrameTextField.getText()));
			mainWindow.dispose();
			master.renderJob(job);
		}
	}

	/**
	 * Method that executes when quit button is pressed
	 */
	private void quitButtonPressed() {
		System.exit(0);
	}

	/**
	 * Method that executes when input file browsing button is pressed
	 * 
	 * @throws InterruptedException
	 * 
	 * @throws InterruptedException
	 */
	private void inputFileButtonPressed() {
		String inputFile = "D:\\Programming\\workspace\\DistributedBlenderer\\etc\\VictorDancing.blend";
		if (inputFile != null) {
			inputFileTextField.setText(inputFile);
			class FrameHelper implements Runnable {
				private String inputFile;

				FrameHelper(String inputFile) {
					this.inputFile = inputFile;
				}

				public void run() {
					frameCount = master.countFrames(inputFile);
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							jProgressBar.setIndeterminate(false);
							jProgressBar.setStringPainted(false);
							endFrameTextField.setText(String
									.valueOf(frameCount));
						}
					});
				}
			}

			jProgressBar.setString("Counting frames ...");
			jProgressBar.setStringPainted(true);
			jProgressBar.setIndeterminate(true);
			FrameHelper fh = new FrameHelper(inputFile);
			Thread thFrames = new Thread(fh);
			thFrames.start();

		}
	}

	/**
	 * Method that executes when output location browsing button is pressed
	 */
	private void outputLocButtonPressed() {
		String outputLoc = BlenderFileChooser.saveBlendFile();
		if (outputLoc != null) {
			outputLocTextField.setText(outputLoc);
		}
	}

	/**
	 * This method initializes jProgressBar
	 * 
	 * @return javax.swing.JProgressBar
	 */
	private JProgressBar getJProgressBar() {
		if (jProgressBar == null) {
			jProgressBar = new JProgressBar();
			jProgressBar.setBounds(new Rectangle(26, 201, 370, 14));
		}
		return jProgressBar;
	}
}
