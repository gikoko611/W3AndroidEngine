package com.etheller.warsmash.viewer5.handlers.w3x.ui.dialog;

import java.util.ArrayList;
import java.util.List;

import com.etheller.interpreter.ast.scope.trigger.RemovableTriggerEvent;
import com.etheller.interpreter.ast.scope.trigger.Trigger;
import com.etheller.interpreter.ast.util.AllocatedHandle;
import com.etheller.warsmash.parsers.fdf.GameUI;
import com.etheller.warsmash.parsers.fdf.frames.GlueTextButtonFrame;
import com.etheller.warsmash.parsers.fdf.frames.StringFrame;
import com.etheller.warsmash.parsers.jass.scope.CommonTriggerExecutionScope;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.JassGameEventsWar3;

public class CScriptDialogButton extends AllocatedHandle {
	private GlueTextButtonFrame buttonFrame;
	private StringFrame buttonText;
	private String text;
	private int hotkey;
	private boolean quitButton;
	private final List<Trigger> eventTriggers = new ArrayList<>();
	private Runnable afterClick;

	public CScriptDialogButton(final GlueTextButtonFrame buttonFrame, final StringFrame buttonText,
			final String text, final int hotkey) {
		this.buttonFrame = buttonFrame;
		this.buttonText = buttonText;
		this.text = text;
		this.hotkey = hotkey;
	}

	/** A saved game keeps what the button says and does; the loaded game gives it new frames. */
	public void attachFrames(final GlueTextButtonFrame buttonFrame, final StringFrame buttonText) {
		this.buttonFrame = buttonFrame;
		this.buttonText = buttonText;
	}

	public String getText() {
		return this.text;
	}

	public int getHotkey() {
		return this.hotkey;
	}

	public boolean isQuitButton() {
		return this.quitButton;
	}

	public GlueTextButtonFrame getButtonFrame() {
		return this.buttonFrame;
	}

	public void setText(final GameUI rootFrame, final String text) {
		this.text = text;
		rootFrame.setText(this.buttonText, text);
	}

	/** A press is a player command: the game clicks the button when it applies that command. */
	public void setupEvents(final Runnable onPress) {
		this.buttonFrame.setOnClick(onPress);
	}

	public void click(final CScriptDialog dialog) {
		for (final Trigger trigger : this.eventTriggers) {
			final CommonTriggerExecutionScope scope = CommonTriggerExecutionScope
					.triggerDialogScope(JassGameEventsWar3.EVENT_DIALOG_BUTTON_CLICK, trigger, dialog, this);
			dialog.getGlobalScope().queueTrigger(null, null, trigger, scope, scope);
		}
		dialog.onButtonClick(this);
		if (this.afterClick != null) {
			this.afterClick.run();
		}
	}

	/**
	 * Runs after the button's triggers are queued, as a quit button (DialogAddQuitButton) leaves the game then.
	 */
	public void setQuitAction(final Runnable afterClick) {
		this.quitButton = true;
		this.afterClick = afterClick;
	}

	public RemovableTriggerEvent addEvent(final Trigger trigger) {
		this.eventTriggers.add(trigger);
		return new RemovableTriggerEvent(trigger) {
			@Override
			public void remove() {
				CScriptDialogButton.this.eventTriggers.remove(trigger);
			}
		};
	}
}
