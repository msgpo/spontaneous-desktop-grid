package ee.ut.xpp2p.ui;

import java.io.File;

import ee.ut.xpp2p.communicator.BlenderCommunicator;
import ee.ut.xpp2p.model.Job;

/**
 * Class that checks input data and reacts to ui-events
 * 
 * @author Jaan Neljandik
 * @created 27.10.2007 
 */
public class InputValidator {

	/**
	 * Method that executes when render button is pressed
	 */
	public void renderButtonPressed() {
		boolean hasErrors = false;

		// Validates input file
		String inputFile = MainWindow.inputFileTextField.getText();
		hasErrors = hasErrors || validateInputFile(inputFile);

		// Validates output location
		String outputLoc = MainWindow.outputLocTextField.getText();
		hasErrors = hasErrors || validateOutputLoc(outputLoc);

		// Validates frames
		String startFrame = MainWindow.startFrameTextField.getText();
		String endFrame = MainWindow.endFrameTextField.getText();
		hasErrors = hasErrors || validateFrameNumbers(startFrame, endFrame);

		// Renders
		if (!hasErrors) {
			String fileType = MainWindow.filetypeChoice.getSelectedItem();

			Job job = new Job();
			job.setInputFile(inputFile);
			job.setOutputLocation(outputLoc);
			job.setOutputFormat(fileType);
			job.setStartFrame(Long.parseLong(startFrame));
			job.setEndFrame(Long.parseLong(endFrame));
			// TODO: Get number of participants
			job.setParticipants(2);

			MainWindow.disposeMainWindow();
			BlenderCommunicator.renderJob(job);

			// Exit
			System.exit(0);
		}

	}

	/**
	 * Method that executes when quit button is pressed
	 */
	public void quitButtonPressed() {
		System.exit(0);
	}

	/**
	 * Method that executes when input file browsing button is pressed
	 */
	public void inputFileButtonPressed() {
		String inputFile = BlenderFileChooser.openBlendFile();
		if (inputFile != null) {
			MainWindow.inputFileTextField.setText(inputFile);
			long frameCount = BlenderCommunicator.countFrames(inputFile);
			MainWindow.endFrameTextField.setText(String.valueOf(frameCount));
		}
	}

	/**
	 * Method that executes when output location browsing button is pressed
	 */
	public void outputLocButtonPressed() {
		String outputLoc = BlenderFileChooser.saveBlendFile();
		if (outputLoc != null) {
			MainWindow.outputLocTextField.setText(outputLoc);
		}
	}

	/**
	 * Validates inputFile
	 * 
	 * @param inputFile
	 *            input file
	 * @return <code>true</code>if input file has errors
	 */
	private boolean validateInputFile(String inputFile) {
		boolean hasErrors = false;

		if (inputFile.equals("")) {
			MainWindow.inputFileErrorLabel
					.setText("Input file must be specified");
			hasErrors = true;
		} else if (!new File(inputFile).exists()) {
			MainWindow.inputFileErrorLabel.setText("Input file doesn't exist");
			hasErrors = true;
		} else {
			MainWindow.inputFileErrorLabel.setText("");
		}
		return hasErrors;
	}

	/**
	 * Validates output location
	 * 
	 * @param outputLoc
	 *            ouutput location
	 * @return <code>true</code>if output location has errors
	 */
	private boolean validateOutputLoc(String outputLoc) {
		boolean hasErrors = false;

		if (outputLoc.equals("")) {
			MainWindow.outputLocErrorLabel
					.setText("Output location must be specified");
			hasErrors = true;
		} else if (!new File(outputLoc).exists()) {
			MainWindow.outputLocErrorLabel
					.setText("Output location doesn't exist");
			hasErrors = true;
		} else if (!new File(outputLoc).isDirectory()) {
			MainWindow.outputLocErrorLabel
					.setText("Output location must be a directory");
			hasErrors = true;
		} else {
			MainWindow.outputLocErrorLabel.setText("");
		}
		return hasErrors;
	}

	/**
	 * Validates frame numbers
	 * 
	 * @param startFrame
	 *            start frame
	 * @param endFrame
	 *            end frame
	 * @return <code>true</code>if frame numbers have errors
	 */
	private boolean validateFrameNumbers(String startFrame, String endFrame) {
		boolean hasErrors = false;

		if (startFrame.equals("")) {
			MainWindow.framesErrorLabel
					.setText("Start frame must be specified");
			hasErrors = true;
		} else if (endFrame.equals("")) {
			MainWindow.framesErrorLabel.setText("End frame must be specified");
			hasErrors = true;
		} else if (!startFrame.matches("\\d*") || !startFrame.matches("\\d*")) {
			MainWindow.framesErrorLabel
					.setText("Frames must be specified as integers");
			hasErrors = true;
		} else {
			MainWindow.framesErrorLabel.setText("");
		}
		return hasErrors;
	}
}
