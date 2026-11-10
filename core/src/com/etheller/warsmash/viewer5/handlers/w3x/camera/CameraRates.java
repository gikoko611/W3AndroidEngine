package com.etheller.warsmash.viewer5.handlers.w3x.camera;

public class CameraRates {
	public final float aoa;
	public final float fov;
	public final float rotation;
	public final float distance;
	public final float forward;
	public final float strafe;
	public final float roll;
	public final float localPitch;
	public final float localYaw;
	public final float localRoll;

	public CameraRates(final float aoa, final float fov, final float rotation, final float distance,
			final float forward, final float strafe) {
		this(aoa, fov, rotation, distance, forward, strafe, aoa, aoa, aoa, aoa);
	}

	public CameraRates(final float aoa, final float fov, final float rotation, final float distance,
			final float forward, final float strafe, final float roll, final float localPitch,
			final float localYaw, final float localRoll) {
		this.aoa = aoa;
		this.fov = fov;
		this.rotation = rotation;
		this.distance = distance;
		this.forward = forward;
		this.strafe = strafe;
		this.roll = roll;
		this.localPitch = localPitch;
		this.localYaw = localYaw;
		this.localRoll = localRoll;
	}

	public static final CameraRates INFINITY = new CameraRates(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
			Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
}
