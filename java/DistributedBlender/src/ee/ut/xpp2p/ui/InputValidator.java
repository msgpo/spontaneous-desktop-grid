package ee.ut.xpp2p.ui;

import java.io.File;

/**
 * Class that checks input data and reacts to ui-events
 * 
 * @author Jaan Neljandik
 * @created 27.10.2007 
 */
public class InputValidator {
	MainWindow window;
	
	/**
	 * Method that executes when render button is pressed
	 * @return <code>true</code> if input was valid
	 */
	public boolean validate(MainWindow window) {
		this.window = window;
		
		boolean hasErrors = false;

		// Validates input file
		String inputFile = window.inputFileTextField.getText();
		hasErrors = hasErrors | validateInputFile(inputFile);

		// Validates output location
		String outputLoc = window.outputLocTextField.getText();
		hasErrors = hasErrors | validateOutputLoc(outputLoc);

		// Validates frames
		String startFrame = window.startFrameTextField.getText();
		String endFrame = window.endFrameTextField.getText();
		hasErrors = hasErrors | validateFrameNumbers(startFrame, endFrame);

		if (!hasErrors) {
			return true;			
		}
		else{
			return false;
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
			window.inputFileErrorLabel
					.setText("Input file must be specified");
			hasErrors = true;
		} else if (!new File(inputFile).exists()) {
			window.inputFileErrorLabel.setText("Input file doesn't exist");
			hasErrors = true;
		} else {
			window.inputFileErrorLabel.setText("");
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
			window.outputLocErrorLabel
					.setText("Output location must be specified");
			hasErrors = true;
		} else if (!new File(outputLoc).exists()) {
			window.outputLocErrorLabel
					.setText("Output location doesn't exist");
			hasErrors = true;
		} else if (!new File(outputLoc).isDirectory()) {
			window.outputLocErrorLabel
					.setText("Output location must be a directory");
			hasErrors = true;
		} else if (!new File(outputLoc).canWrite()) {
			window.outputLocErrorLabel
					.setText("No write permission on the specified output location");
			hasErrors = true;
		} else {
			window.outputLocErrorLabel.setText("");
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
			window.framesErrorLabel
					.setText("Start frame must be specified");
			hasErrors = true;
		} else if (endFrame.equals("")) {
			window.framesErrorLabel.setText("End frame must be specified");
			hasErrors = true;
		} else if (!startFrame.matches("\\d*") || !endFrame.matches("\\d*")) {
			window.framesErrorLabel
					.setText("Frames must be specified as integers");
			hasErrors = true;
		} else if ( Integer.parseInt(startFrame) > Integer.parseInt(endFrame)){
			window.framesErrorLabel.setText("Starting frame number can't be bigger than ending frame number");
			hasErrors = true;
		}
		else {
			window.framesErrorLabel.setText("");
		}
		return hasErrors;
	}
}
