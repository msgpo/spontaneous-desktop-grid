package ee.ut.xpp2p.ui;

import java.awt.event.ActionEvent;

import junit.framework.TestCase;

/**
 * @author Jürmo
 * @created 03.11.2007
 */
public class InputValidatorTest extends TestCase {

	/**
	 * Test method for {@link ee.ut.xpp2p.ui.InputValidator#renderButtonPressed()}.
	 */
	public void testRenderButtonPressed() {
		// test selecting a valid input file
		MainWindow.initMainWindow();
		MainWindow.inputFileTextField.setText("etc\\VictorDancing.blend");
		MainWindow.renderButton.getActionListeners()[0].actionPerformed(new ActionEvent(MainWindow.renderButton, 2, ""));
		assertEquals(MainWindow.inputFileErrorLabel.getText(), "");
		assertEquals(MainWindow.inputFileTextField.getText(), "etc\\VictorDancing.blend");
		MainWindow.disposeMainWindow();
		//TODO: teardowni + teiste parameetrite jaoks ka
		MainWindow.inputFileTextField.setText("");
		
		// test selecting a valid output location
		MainWindow.initMainWindow();
		MainWindow.outputLocTextField.setText("etc/");
		MainWindow.renderButton.getActionListeners()[0].actionPerformed(new ActionEvent(MainWindow.renderButton, 2, ""));
		assertEquals(MainWindow.outputLocErrorLabel.getText(), "");
		assertEquals(MainWindow.outputLocTextField.getText(), "etc/");
		MainWindow.disposeMainWindow();
		
		// test startFrame number less than endFrame number
		MainWindow.initMainWindow();
		MainWindow.startFrameTextField.setText("5");
		MainWindow.endFrameTextField.setText("3");
		MainWindow.renderButton.getActionListeners()[0].actionPerformed(new ActionEvent(MainWindow.renderButton, 2, ""));
		assertEquals(MainWindow.framesErrorLabel.getText(), "Starting frame number can't be bigger than ending frame number");
		assertEquals(MainWindow.startFrameTextField.getText(), "5");
		assertEquals(MainWindow.endFrameTextField.getText(), "3");
		MainWindow.disposeMainWindow();
	}

}
