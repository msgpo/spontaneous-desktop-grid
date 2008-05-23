package ee.ut.f2f.visualizer.command;

/**
 * Interface defining the application's command IDs.
 * 
 * Key bindings can be defined for specific commands (plugin.xml). To associate
 * an action with a command, use IAction.setActionDefinitionId(commandId).
 * 
 * @see org.eclipse.jface.action.IAction#setActionDefinitionId(String)
 */
public interface ICommandIds {
	
	/** Command for "Open file" action */
	public static final String CMD_OPEN_FILE = "ee.ut.f2f.visualizer.openFile";
	/** Command for "Save as" action */
	public static final String CMD_SAVE_FILE = "ee.ut.f2f.visualizer.saveFile";
	/** Command for "Collect data" action */
	public static final String CMD_COLLECT_DATA = "ee.ut.f2f.visualizer.collectData";
	/** Command for "Switch layout algorithm" action. Used in plugin.xml */
	public static final String CMD_SWITCH_ALGORITHM = "ee.ut.f2f.visualizer.switchAlgorithm";
	
}
