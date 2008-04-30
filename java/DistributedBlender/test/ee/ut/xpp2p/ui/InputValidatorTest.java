package ee.ut.xpp2p.ui;

import java.awt.event.ActionEvent;
import java.io.File;

import junit.framework.TestCase;
import ee.ut.xpp2p.blenderer.BlenderMasterTask;

/**
 * @author Jürmo
 * @created 03.11.2007
 */
public class InputValidatorTest extends TestCase {

	static MainWindow window = null;

	/**
	 * Test method for {@link ee.ut.xpp2p.ui.InputValidator#validate()}. Tested
	 * through the user interface MainWindow
	 */

	// testing valid input parameters
	public void testSelectingValidInputFile() {
		window.inputFileTextField.setText("etc\\VictorDancing.blend");
		window.renderButton.getActionListeners()[0]
				.actionPerformed(new ActionEvent(window.renderButton, 2, ""));
		assertEquals(window.inputFileErrorLabel.getText(), "");
		assertEquals(window.inputFileTextField.getText(),
				"etc\\VictorDancing.blend");
	}
	
	public void testSelectingValidOutputLocation(){
		window.outputLocTextField.setText("etc/");
		window.renderButton.getActionListeners()[0]
				.actionPerformed(new ActionEvent(window.renderButton, 2, ""));
		assertEquals(window.outputLocErrorLabel.getText(), "");
		assertEquals(window.outputLocTextField.getText(), "etc/");
	}
	
	public void testStartFrameEqualsEndFrame(){
		window.startFrameTextField.setText("5");
		window.endFrameTextField.setText("5");
		window.renderButton.getActionListeners()[0]
				.actionPerformed(new ActionEvent(window.renderButton, 2, ""));
		assertEquals(window.framesErrorLabel.getText(), "");
		assertEquals(window.startFrameTextField.getText(), "5");
		assertEquals(window.endFrameTextField.getText(), "5");
	}
	
	public void testStartFrameLessThanEndFrame(){
		window.startFrameTextField.setText("3");
		window.endFrameTextField.setText("5");
		window.renderButton.getActionListeners()[0]
				.actionPerformed(new ActionEvent(window.renderButton, 2, ""));
		assertEquals(window.framesErrorLabel.getText(), "");
		assertEquals(window.startFrameTextField.getText(), "3");
		assertEquals(window.endFrameTextField.getText(), "5");
	}
	
	public void testNoInputParametersSpecified(){
		window.startFrameTextField.setText("");
		window.renderButton.getActionListeners()[0]
				.actionPerformed(new ActionEvent(window.renderButton, 2, ""));
		assertEquals(window.inputFileErrorLabel.getText(),
				"Input file must be specified");
		assertEquals(window.outputLocErrorLabel.getText(),
				"Output location must be specified");
		assertEquals(window.framesErrorLabel.getText(),
				"Start frame must be specified");
	}
	
	public void testNoEndFrameSpecified(){
		window.endFrameTextField.setText("");
		window.renderButton.getActionListeners()[0]
				.actionPerformed(new ActionEvent(window.renderButton, 2, ""));
		assertEquals(window.framesErrorLabel.getText(),	"End frame must be specified");
	}
	
	public void testNonexistentInputFile(){
		window.inputFileTextField.setText("etc\\nonexistent.blend");
		window.renderButton.getActionListeners()[0].actionPerformed(new ActionEvent(window.renderButton, 2, ""));
		assertEquals(window.inputFileErrorLabel.getText(), "Input file doesn't exist");
	}
	
	public void testNonexistentOutputFolder(){
		window.outputLocTextField.setText("nonexistent/");
		window.renderButton.getActionListeners()[0]
				.actionPerformed(new ActionEvent(window.renderButton, 2, ""));
		assertEquals(window.outputLocErrorLabel.getText(),
				"Output location doesn't exist");
	}
	
	public void testNoWritePermissionOnOutputFolder(){
		window.outputLocTextField.setText("etc/readOnlyLocation/");
		new File("etc/readOnlyLocation").setReadOnly();
		window.renderButton.getActionListeners()[0]
				.actionPerformed(new ActionEvent(window.renderButton, 2, ""));
		assertEquals(window.outputLocErrorLabel.getText(),
				"No write permission on the specified output location");
	}
	
	public void testNonnumericStartFrame(){
		window.startFrameTextField.setText("text");
		window.renderButton.getActionListeners()[0]
				.actionPerformed(new ActionEvent(window.renderButton, 2, ""));
		assertEquals(window.framesErrorLabel.getText(),
				"Frames must be specified as integers");
	}
	
	public void testNonnumericEndFrame(){
		window.endFrameTextField.setText("text");
		window.renderButton.getActionListeners()[0]
				.actionPerformed(new ActionEvent(window.renderButton, 2, ""));
		assertEquals(window.framesErrorLabel.getText(),
				"Frames must be specified as integers");
	}

	public void testStartFrameBiggerThanEndFrame() {
		window.startFrameTextField.setText("5");
		window.endFrameTextField.setText("3");
		window.renderButton.getActionListeners()[0]
				.actionPerformed(new ActionEvent(window.renderButton, 2, ""));
		assertEquals(window.framesErrorLabel.getText(),
				"Starting frame number can't be bigger than ending frame number");
		assertEquals(window.startFrameTextField.getText(), "5");
		assertEquals(window.endFrameTextField.getText(), "3");
	}

	protected void setUp() {
		window = new MainWindow(new BlenderMasterTask());
	}

	protected void tearDown() {
		window.dispose();
		window.inputFileTextField.setText("");
		window.outputLocTextField.setText("");
		window.startFrameTextField.setText("");
		window.endFrameTextField.setText("");
		window.inputFileErrorLabel.setText("");
		window.outputLocErrorLabel.setText("");
		window.framesErrorLabel.setText("");
	}

}
