package ee.ut.xpp2p.ui;

import java.awt.event.ActionEvent;

import junit.framework.TestCase;
import ee.ut.xpp2p.blenderer.MasterBlenderer;

/**
 * @author Jürmo
 * @created 03.11.2007
 */
public class InputValidatorTest extends TestCase {
	
	static MainWindow window = null;

	/**
	 * Test method for {@link ee.ut.xpp2p.ui.InputValidator#validate()}.
	 */
	public void testRenderButtonPressed() {
		// testing valid input parameters
		
		// test selecting a valid input file
		setUp();
		window.inputFileTextField.setText("etc\\VictorDancing.blend");
		window.renderButton.getActionListeners()[0].actionPerformed(new ActionEvent(window.renderButton, 2, ""));
		assertEquals(window.inputFileErrorLabel.getText(), "");
		assertEquals(window.inputFileTextField.getText(), "etc\\VictorDancing.blend");
		tearDown();
		
		// test selecting a valid output location
		setUp();
		window.outputLocTextField.setText("etc/");
		window.renderButton.getActionListeners()[0].actionPerformed(new ActionEvent(window.renderButton, 2, ""));
		assertEquals(window.outputLocErrorLabel.getText(), "");
		assertEquals(window.outputLocTextField.getText(), "etc/");
		tearDown();

		// test startFrame number equals endFrame number
		setUp();
		window.startFrameTextField.setText("5");
		window.endFrameTextField.setText("5");
		window.renderButton.getActionListeners()[0].actionPerformed(new ActionEvent(window.renderButton, 2, ""));
		assertEquals(window.framesErrorLabel.getText(), "");
		assertEquals(window.startFrameTextField.getText(), "5");
		assertEquals(window.endFrameTextField.getText(), "5");
		tearDown();
		
		// test startFrame number bigger than endFrame number
		setUp();
		window.startFrameTextField.setText("5");
		window.endFrameTextField.setText("3");
		window.renderButton.getActionListeners()[0].actionPerformed(new ActionEvent(window.renderButton, 2, ""));
		assertEquals(window.framesErrorLabel.getText(), "Starting frame number can't be bigger than ending frame number");
		assertEquals(window.startFrameTextField.getText(), "5");
		assertEquals(window.endFrameTextField.getText(), "3");
		tearDown();
		
		// testing empty input parameters
		
		// test no input file, output location or startFrame specified
		setUp();
		window.startFrameTextField.setText("");
		window.renderButton.getActionListeners()[0].actionPerformed(new ActionEvent(window.renderButton, 2, ""));
		assertEquals(window.inputFileErrorLabel.getText(), "Input file must be specified");
		assertEquals(window.outputLocErrorLabel.getText(), "Output location must be specified");
		assertEquals(window.framesErrorLabel.getText(), "Start frame must be specified");
		tearDown();
		
		// test no endFrame specified
		setUp();
		window.endFrameTextField.setText("");
		window.renderButton.getActionListeners()[0].actionPerformed(new ActionEvent(window.renderButton, 2, ""));
		assertEquals(window.framesErrorLabel.getText(), "End frame must be specified");
		tearDown();
		
		//TODO: test  invalid input parameters
		
		// test nonexistent input file
		setUp();
		window.inputFileTextField.setText("etc\\nonexistent.blend");
		window.renderButton.getActionListeners()[0].actionPerformed(new ActionEvent(window.renderButton, 2, ""));
		assertEquals(window.inputFileErrorLabel.getText(), "Input file doesn't exist");
		tearDown();
		
		//TODO: nonexistent folder
		//TODO: no write permission on folder
		//TODO: existing folder no slash
		//TODO: non-numeric startframe
		//TODO: non-numeric endframe
		
		// test startFrame number bigger than endFrame number
		setUp();
		window.startFrameTextField.setText("3");
		window.endFrameTextField.setText("5");
		window.renderButton.getActionListeners()[0].actionPerformed(new ActionEvent(window.renderButton, 2, ""));
		assertEquals(window.framesErrorLabel.getText(), "");
		assertEquals(window.startFrameTextField.getText(), "3");
		assertEquals(window.endFrameTextField.getText(), "5");
		tearDown();
		

	}
	
	protected void setUp(){
		window = new MainWindow(new MasterBlenderer());
	}
	
	protected void tearDown(){
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
