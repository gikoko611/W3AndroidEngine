package com.etheller.warsmash.viewer5.handlers.w3x.simulation.players;

import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.scope.trigger.Trigger;
import com.etheller.warsmash.parsers.jass.scope.CommonTriggerExecutionScope;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.JassGameEventsWar3;

/**
 * TriggerRegisterPlayerChatEvent: a message the player sends, matched whole or as a substring. An empty match string
 * takes every message (the usual way scripts read all chat).
 */
public class CPlayerChatEvent extends CPlayerEvent {
	private final GlobalScope globalScope;
	private final CPlayerJass player;
	private final String chatMessageToDetect;
	private final boolean exactMatchOnly;

	public CPlayerChatEvent(final GlobalScope globalScope, final CPlayerJass player, final Trigger trigger,
			final String chatMessageToDetect, final boolean exactMatchOnly) {
		super(globalScope, player, trigger, JassGameEventsWar3.EVENT_PLAYER_CHAT, null);
		this.globalScope = globalScope;
		this.player = player;
		this.chatMessageToDetect = chatMessageToDetect == null ? "" : chatMessageToDetect;
		this.exactMatchOnly = exactMatchOnly;
	}

	public boolean matches(final String message) {
		if (this.exactMatchOnly) {
			return message.equals(this.chatMessageToDetect);
		}
		return message.contains(this.chatMessageToDetect);
	}

	public void fireChat(final String message) {
		if (matches(message)) {
			final CommonTriggerExecutionScope scope = CommonTriggerExecutionScope.playerChatScope(getTrigger(),
					this.player, message, this.chatMessageToDetect);
			this.globalScope.queueTrigger(null, null, getTrigger(), scope, scope);
		}
	}
}
