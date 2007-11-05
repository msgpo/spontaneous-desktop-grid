package ee.ut.xpp2p.ui;

import java.awt.event.ActionEvent;

import junit.framework.TestCase;
import ee.ut.xpp2p.blenderer.MasterBlenderer;

/**
 * @author Jürmo
 * @created 03.11.2007
 */
public class InputValidatorTest extends TestCase {

	/**
	 * Test method for {@link ee.ut.xpp2p.ui.InputValidator#validate()}.
	 */
	public void testRenderButtonPressed() {
		// test selecting a valid input file
		MainWindow window = new MainWindow(new MasterBlenderer());
		window.inputFileTextField.setText("etc\\VictorDancing.blend");
		window.renderButton.getActionListeners()[0].actionPerformed(new ActionEvent(window.renderButton, 2, ""));
		assertEquals(window.inputFileErrorLabel.getText(), "");
		assertEquals(window.inputFileTextField.getText(), "etc\\VictorDancing.blend");
		window.dispose();
		//TODO: teardowni + teiste parameetrite jaoks ka
		window.inputFileTextField.setText("");
		
		// test selecting a valid output location
		window = new MainWindow(new MasterBlenderer());
		window.outputLocTextField.setText("etc/");
		window.renderButton.getActionListeners()[0].actionPerformed(new ActionEvent(window.renderButton, 2, ""));
		assertEquals(window.outputLocErrorLabel.getText(), "");
		assertEquals(window.outputLocTextField.getText(), "etc/");
		window.dispose();
		
		// test startFrame number less than endFrame number
		window = new MainWindow(new MasterBlenderer());
		window.startFrameTextField.setText("5");
		window.endFrameTextField.setText("3");
		window.renderButton.getActionListeners()[0].actionPerformed(new ActionEvent(window.renderButton, 2, ""));
		assertEquals(window.framesErrorLabel.getText(), "Starting frame number can't be bigger than ending frame number");
		assertEquals(window.startFrameTextField.getText(), "5");
		assertEquals(window.endFrameTextField.getText(), "3");
		window.dispose();
	}

}
