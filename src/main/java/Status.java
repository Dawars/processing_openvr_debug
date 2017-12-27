import com.jogamp.opengl.util.GLBuffers;
import processing.core.PApplet;
import processing.core.PVector;
import vr.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import static vr.VR.ETrackedDeviceClass.TrackedDeviceClass_Controller;
import static vr.VR.ETrackedDeviceClass.TrackedDeviceClass_GenericTracker;
import static vr.VR.ETrackedDeviceClass.TrackedDeviceClass_TrackingReference;
import static vr.VR.ETrackingUniverseOrigin.TrackingUniverseStanding;
import static vr.VR.EVREventType.VREvent_DriverRequestedQuit;
import static vr.VR.EVREventType.VREvent_Quit;
import static vr.VR.k_unMaxTrackedDeviceCount;
import static vr.VR.k_unTrackedDeviceIndex_Hmd;

public class Status extends PApplet {
    private IVRCompositor_FnTable compositor;
    private IVRChaperone_FnTable chaperone;

    private boolean isReady = false; // if vr init is finished

    PVector playArea = new PVector();
    HmdQuad_t playAreaRect = new HmdQuad_t();

    int woldScale = 200;

    @Override
    public void settings() {
        size(1280, 720);
    }

    private static IVRSystem hmd;
    private static IntBuffer errorBuffer = GLBuffers.newDirectIntBuffer(1);

    @Override
    public void setup() {
        background(0);
        fill(255, 255, 255);
        color(255, 255, 255);
        strokeWeight(3);
        stroke(255f);
    }

    @Override
    public void draw() {
        if (!initVr()) return;

        background(0);

        text("PlayArea: " + playArea.x + " m x " + playArea.y + " m", 10, 20);


        for (int i = 0; i <= playAreaRect.vCorners.length; i++) {

            line(width / 2 + playAreaRect.vCorners[i % 4].v[0] * woldScale,
                    height / 2 + playAreaRect.vCorners[i % 4].v[2] * woldScale,
                    width / 2 + playAreaRect.vCorners[(i + 1) % 4].v[0] * woldScale,
                    height / 2 + playAreaRect.vCorners[(i + 1) % 4].v[2] * woldScale);
        }

        getTrackingPosition();


        // process events
        VREvent_t event = new VREvent_t();
        hmd.PollNextEvent.apply(event, event.size());

        switch (event.eventType) {
            //Handle quiting the app from Steam
            case VREvent_DriverRequestedQuit:
            case VREvent_Quit:
                exit();
                break;
        }
    }

    private boolean initVr() {
        if (!isReady) {
            hmd = VR.VR_Init(errorBuffer, VR.EVRApplicationType.VRApplication_Scene);

            int error = errorBuffer.get(0);
            if (error != VR.EVRInitError.VRInitError_None) {

                System.out.println(VR.VR_GetVRInitErrorAsEnglishDescription(error));
                delay(5000);
                return false;
            }

            chaperone = new IVRChaperone_FnTable(VR.VR_GetGenericInterface(VR.IVRChaperone_Version, errorBuffer));
            System.out.println(VR.VR_GetVRInitErrorAsEnglishDescription(error));

            compositor = new IVRCompositor_FnTable(VR.VR_GetGenericInterface(VR.IVRCompositor_Version, errorBuffer));
            System.out.println(VR.VR_GetVRInitErrorAsEnglishDescription(error));


            isReady = true;

            getChaperoneData();
        }
        return true;
    }

    private void getChaperoneData() {
        FloatBuffer w = GLBuffers.newDirectFloatBuffer(1);
        FloatBuffer h = GLBuffers.newDirectFloatBuffer(1);
        chaperone.GetPlayAreaSize.apply(w, h);

        playArea.set(w.get(0), h.get(0));

        chaperone.GetPlayAreaRect.apply(playAreaRect);

    }

    private void connectedDevices() {
        //Iterate through the possible trackedDeviceIndexes
        for (int trackedDevice = k_unTrackedDeviceIndex_Hmd + 1;
             trackedDevice < k_unMaxTrackedDeviceCount;
             trackedDevice++) {
            //If the device is not connected, pass.
            if (!hmd.IsTrackedDeviceConnected.apply(trackedDevice))
                continue;
            //If the device is not recognized as a controller, pass
            if (hmd.GetTrackedDeviceClass.apply(trackedDevice) == TrackedDeviceClass_Controller)
                System.out.println("Controller: " + trackedDevice);
            if (hmd.GetTrackedDeviceClass.apply(trackedDevice) == TrackedDeviceClass_GenericTracker)
                System.out.println("GenericTracker: " + trackedDevice);
            if (hmd.GetTrackedDeviceClass.apply(trackedDevice) == TrackedDeviceClass_TrackingReference)
                System.out.println("Tracker: " + trackedDevice);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        VR.VR_Shutdown();

    }

    public static void main(String[] args) {
        PApplet.main(Status.class);
    }

    /**
     * https://github.com/Omnifinity/OpenVR-Tracking-Example/blob/master/HTC%20Lighthouse%20Tracking%20Example/LighthouseTracking.cpp
     */
    public void getTrackingPosition() {

        for (int unDevice = 0; unDevice < k_unMaxTrackedDeviceCount; unDevice++) {
            // if not connected just skip the rest of the routine
            if (!hmd.IsTrackedDeviceConnected.apply(unDevice))
                continue;

            TrackedDevicePose_t[] trackedDevicePose = new TrackedDevicePose_t[1];

            if (hmd.IsInputFocusCapturedByAnotherProcess.apply()) {
                System.out.println("Input Focus by Another Process");
            }

            // Get what type of device it is and work with its data
            int trackedDeviceClass = hmd.GetTrackedDeviceClass.apply(unDevice);
            switch (trackedDeviceClass) {
                case VR.ETrackedDeviceClass.TrackedDeviceClass_HMD:
                    // print stuff for the HMD here, see controller stuff in next case block

                    // get pose relative to the safe bounds defined by the user
                    hmd.GetDeviceToAbsoluteTrackingPose.apply(TrackingUniverseStanding, 0, trackedDevicePose, 1);

                    HmdMatrix34_t mat = trackedDevicePose[0].mDeviceToAbsoluteTracking;
                    point(width / 2 + mat.m[3] * woldScale, height / 2 + mat.m[11] * woldScale); // hmd working
                    break;

                case TrackedDeviceClass_Controller:
                    VRControllerState_t controllerState = new VRControllerState_t();
                    TrackedDevicePose_t pose = new TrackedDevicePose_t();
                    hmd.GetControllerStateWithPose.apply(TrackingUniverseStanding, unDevice, controllerState, controllerState.size(), pose);
                    mat = pose.mDeviceToAbsoluteTracking;
                    point(width / 2 + mat.m[3] * woldScale, height / 2 + mat.m[11] * woldScale); // hmd working

                    break;
            }
        }
    }
}