package com.etheller.warsmash.desktop.editor.abilitybuilder;

import java.lang.reflect.Field;

import com.etheller.warsmash.util.Log;
import com.etheller.warsmash.viewer5.handlers.w3x.AnimationTokens.SecondaryTag;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbilityCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbilityDisableType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.autocast.AutocastType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.core.ABLocalStoreKeys;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.parser.AbilityBuilderType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.parser.template.DataFieldLetter;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuffType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuffType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeysEnum;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.ResourceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.TextTagConfigType;

public class JassGeneratorForType {
	public static void main(final String[] args) {
		for (final CTargetType type : CTargetType.values()) {
			Log.info(String.format("    constant targettype TARGET_TYPE_%-34s= ConvertTargetType(%d)",
					type.name(), type.ordinal()));
		}
		for (final TextTagConfigType type : TextTagConfigType.values()) {
			Log.info(String.format(
					"    constant texttagconfigtype TEXT_TAG_CONFIG_TYPE_%-34s= ConvertTextTagConfigType(%d)",
					type.name(), type.ordinal()));
		}
		for (final NonStackingStatBuffType type : NonStackingStatBuffType.values()) {
			Log.info(String.format(
					"    constant nonstackingstatbufftype NON_STACKING_STAT_BUFF_TYPE_%-34s= ConvertNonStackingStatBuffType(%d)",
					type.name(), type.ordinal()));
		}
		for (final StateModBuffType type : StateModBuffType.values()) {
			Log.info(String.format("    constant statemodtype STATE_MOD_TYPE_%-34s= ConvertStateModType(%d)",
					type.name(), type.ordinal()));
		}
		for (final DataFieldLetter type : DataFieldLetter.values()) {
			Log.info(
					String.format("    constant datafieldletter DATA_FIELD_LETTER_%-34s= ConvertDataFieldLetter(%d)",
							type.name(), type.ordinal()));
		}
		for (final AutocastType type : AutocastType.values()) {
			Log.info(String.format("    constant autocasttype AUTOCAST_TYPE_%-34s= ConvertAutocastType(%d)",
					type.name(), type.ordinal()));
		}
		for (final AbilityBuilderType type : AbilityBuilderType.values()) {
			Log.info(String.format("    constant abconftype AB_CONF_TYPE_%-34s= ConvertABConfType(%d)",
					type.name(), type.ordinal()));
		}
		for (final CAbilityDisableType type : CAbilityDisableType.values()) {
			Log.info(String.format(
					"    constant abilitydisabletype ABILITY_DISABLE_TYPE_%-34s= ConvertAbilityDisableType(%d)",
					type.name(), type.ordinal()));
		}
		for (final ResourceType type : ResourceType.values()) {
			Log.info(String.format("    constant resourcetype RESOURCE_TYPE_%-34s= ConvertResourceType(%d)",
					type.name(), type.ordinal()));
		}
		for (final CBehaviorCategory type : CBehaviorCategory.values()) {
			Log.info(
					String.format("    constant behaviorcategory BEHAVIOR_CATEGORY_%-34s= ConvertBehaviorCategory(%d)",
							type.name(), type.ordinal()));
		}
		for (final CAbilityCategory type : CAbilityCategory.values()) {
			Log.info(
					String.format("    constant abilitycategory Ability_CATEGORY_%-34s= ConvertAbilityCategory(%d)",
							type.name(), type.ordinal()));
		}
		for (final CommandStringErrorKeysEnum type : CommandStringErrorKeysEnum.values()) {
			Log.info(String.format("    constant string COMMAND_STRING_ERROR_KEY_%-85s= \"%s\"", type.name(),
					type.getKey()));
		}
		for (final SecondaryTag type : SecondaryTag.values()) {
			Log.info(String.format("    public static constant secondarytag %-12s= ConvertSecondaryTag(%d)",
					type.name(), type.ordinal()));
		}

		{
			final Field[] fields = ABLocalStoreKeys.class.getDeclaredFields();
			for (final Field field : fields) {
				try {
					Log.info(String.format("    constant string AB_LOCAL_STORE_KEY_%-34s= \"%s\"",
							field.getName(), field.get(null)));
				}
				catch (final IllegalArgumentException e) {
					// TODO Auto-generated catch block
					Log.error(e);
				}
				catch (final IllegalAccessException e) {
					// TODO Auto-generated catch block
					Log.error(e);
				}
			}
		}

		{
			final Field[] fields = AbilityFields.class.getDeclaredFields();
			for (final Field field : fields) {
				try {
					Log.info(String.format("    constant string ABILITY_FIELD_%-34s= \"%s\"", field.getName(),
							field.get(null)));
				}
				catch (final IllegalArgumentException e) {
					// TODO Auto-generated catch block
					Log.error(e);
				}
				catch (final IllegalAccessException e) {
					// TODO Auto-generated catch block
					Log.error(e);
				}
			}
		}
	}
}
