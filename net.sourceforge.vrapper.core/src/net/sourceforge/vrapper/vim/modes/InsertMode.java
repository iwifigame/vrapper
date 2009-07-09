
package net.sourceforge.vrapper.vim.modes;

import static net.sourceforge.vrapper.keymap.vim.ConstructorWrappers.key;
import net.sourceforge.vrapper.keymap.KeyMap;
import net.sourceforge.vrapper.keymap.KeyStroke;
import net.sourceforge.vrapper.keymap.SpecialKey;
import net.sourceforge.vrapper.platform.KeyMapProvider;
import net.sourceforge.vrapper.platform.TextContent;
import net.sourceforge.vrapper.utils.CaretType;
import net.sourceforge.vrapper.utils.ContentType;
import net.sourceforge.vrapper.utils.Position;
import net.sourceforge.vrapper.utils.StartEndTextRange;
import net.sourceforge.vrapper.utils.TextRange;
import net.sourceforge.vrapper.vim.EditorAdaptor;
import net.sourceforge.vrapper.vim.VimConstants;
import net.sourceforge.vrapper.vim.commands.Command;
import net.sourceforge.vrapper.vim.commands.CommandExecutionException;
import net.sourceforge.vrapper.vim.commands.CountIgnoringNonRepeatableCommand;
import net.sourceforge.vrapper.vim.commands.MotionCommand;
import net.sourceforge.vrapper.vim.commands.PasteBeforeCommand;
import net.sourceforge.vrapper.vim.commands.SwitchRegisterCommand;
import net.sourceforge.vrapper.vim.commands.VimCommandSequence;
import net.sourceforge.vrapper.vim.commands.motions.MoveLeft;
import net.sourceforge.vrapper.vim.register.Register;
import net.sourceforge.vrapper.vim.register.RegisterContent;
import net.sourceforge.vrapper.vim.register.StringRegisterContent;

public class InsertMode extends AbstractMode {


    public static final String NAME = "insert mode";
    public static final String KEYMAP_NAME = "Insert Mode Keymap";
    // FIXME: change this to option some day
    public static final boolean CHANGES_ARE_ATOMIC = false;

    private Position startEditPosition;

    /**
     * Command to be used before insertion
     */
    private Command command;
    private int count;

    public InsertMode(EditorAdaptor editorAdaptor) {
        super(editorAdaptor);
    }

    public String getName() {
        return NAME;
    }

    /**
     * @param args command to perform on entering insert mode
     */
    public void enterMode(Object... args) {
        if (isEnabled) {
            return;
        }
        count = 1;
        if (args.length > 0) {
            command = (Command) args[0];
            if (command != null) {
                try {
                    command.execute(editorAdaptor);
                } catch (CommandExecutionException e) {
                    editorAdaptor.getUserInterfaceService().setErrorMessage(e.getMessage());
                }
            }
            if (args.length > 1) {
                count = ((Integer) args[1]).intValue();
            }
        } else {
            command = null;
        }
        isEnabled = true;
        if (CHANGES_ARE_ATOMIC) {
            editorAdaptor.getHistory().beginCompoundChange();
            editorAdaptor.getHistory().lock();
        }
        editorAdaptor.getCursorService().setCaret(CaretType.VERTICAL_BAR);
        startEditPosition = editorAdaptor.getCursorService().getPosition();
    }

    public void leaveMode() {
        isEnabled = false;
        saveTypedText();
        try {
            MotionCommand.doIt(editorAdaptor, new MoveLeft());
        } catch (CommandExecutionException e) {
            editorAdaptor.getUserInterfaceService().setErrorMessage(e.getMessage());
        }
        repeatInsert();
        if (CHANGES_ARE_ATOMIC) {
            editorAdaptor.getHistory().unlock();
            editorAdaptor.getHistory().endCompoundChange();
        }
    }

    private void repeatInsert() {
        if (count > 1) {
            try {
                editorAdaptor.getRegisterManager().getLastEdit().withCount(
                        count - 1).execute(editorAdaptor);
            } catch (CommandExecutionException e) {
                editorAdaptor.getUserInterfaceService().setErrorMessage(e.getMessage());
            }
        }
    }

    private void saveTypedText() {
        Register lastEditRegister = editorAdaptor.getRegisterManager().getLastEditRegister();
        TextContent content = editorAdaptor.getModelContent();
        Position position = editorAdaptor.getCursorService().getPosition();
        TextRange editRange = new StartEndTextRange(startEditPosition, position);
        String text = content.getText(editRange.getLeftBound().getModelOffset(), editRange.getViewLength());
        Command repetition;
        RegisterContent registerContent = new StringRegisterContent(ContentType.TEXT, text);
        lastEditRegister.setContent(registerContent);
        if (command != null) {
            Command newCommand = command.repetition();
            if (newCommand == null) {
                newCommand = command;
            }
            repetition = new VimCommandSequence(
                    newCommand,
                    new SwitchRegisterCommand(lastEditRegister),
                    new PasteBeforeCommand(),
                    new MoveRightOverLineBreak(text.length()-1));
        } else {
            repetition = new SimpleInsertCommandSequence(
                    new SwitchRegisterCommand(lastEditRegister),
                    new PasteBeforeCommand(),
                    new MoveRightOverLineBreak(text.length()-1));
        }
        editorAdaptor.getRegisterManager().setLastEdit(
                count > 1 ? repetition.withCount(count) : repetition);
    }

    public boolean handleKey(KeyStroke stroke) {
        if (stroke.equals(key(SpecialKey.ESC)) || stroke.equals(key(KeyStroke.CTRL, '['))) {
            editorAdaptor.changeMode(NormalMode.NAME);
            return true;
        }
        else if (!allowed(stroke)) {
            startEditPosition = editorAdaptor.getCursorService().getPosition();
        } else if (stroke.isVirtual()) {
            // stroke was generated by Vrapper, it will not be passed to the editor
            Position pos = editorAdaptor.getPosition();
            String s = String.valueOf(stroke.getCharacter());
            // TDOO: use smart insert and find a way to calculate the correct position
            editorAdaptor.getModelContent().replace(pos.getModelOffset(), 0, s);
            editorAdaptor.setPosition(pos.addModelOffset(1), false);
        }
        return false;
    }


    private boolean allowed(KeyStroke stroke) {
        // TODO: option to allow arrows
        SpecialKey specialKey = stroke.getSpecialKey();
        if (specialKey != null) {
            return VimConstants.SPECIAL_KEYS_ALLOWED_FOR_INSERT.contains(specialKey);
        }
        if ((stroke.getModifiers() & KeyStroke.CTRL) == 0) {
            return true; // FIXME: look one line below
        }
        //			return Character.isLetterOrDigit(event.character); // FIXME: no enter, no backspace
        return false;
    }

    public KeyMap resolveKeyMap(KeyMapProvider provider) {
        return provider.getKeyMap(KEYMAP_NAME);
    }

    public class MoveRightOverLineBreak extends CountIgnoringNonRepeatableCommand {

        private final int offset;

        public MoveRightOverLineBreak(int offset) {
            super();
            this.offset = offset;
        }

        public void execute(EditorAdaptor editorAdaptor)
                throws CommandExecutionException {
            editorAdaptor.setPosition(
                    editorAdaptor.getPosition().addModelOffset(offset), true);
        }
    }

    public class SimpleInsertCommandSequence extends VimCommandSequence {

        public SimpleInsertCommandSequence(Command... commands) {
            super(commands);
        }

        @Override
        public Command withCount(final int count) {
            return new CountIgnoringNonRepeatableCommand() {

                public void execute(EditorAdaptor editorAdaptor) throws CommandExecutionException {
                    SimpleInsertCommandSequence.this.execute(editorAdaptor);
                    for (int i = 1; i < count; i++) {
                        Position pos = editorAdaptor.getPosition();
                        editorAdaptor.setPosition(pos.addModelOffset(1), false);
                        SimpleInsertCommandSequence.this.execute(editorAdaptor);
                    }
                }
            };
        }

    }
}