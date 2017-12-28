import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.util.GLBuffers;
import processing.core.*;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.PJOGL;
import processing.opengl.Texture;
import vr.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;

import static vr.VR.ETrackedDeviceClass.TrackedDeviceClass_Controller;
import static vr.VR.ETrackedDeviceProperty.*;
import static vr.VR.ETrackedDeviceProperty.Prop_NamedIconPathDeviceAlertLow_String;
import static vr.VR.ETrackingUniverseOrigin.TrackingUniverseStanding;
import static vr.VR.EVREventType.*;
import static vr.VR.k_unMaxTrackedDeviceCount;
import static vr.VR.k_unTrackedDeviceIndex_Hmd;

public class Status extends PApplet {
    private IVRCompositor_FnTable compositor;
    private IVRChaperone_FnTable chaperone;

    private boolean isReady = false; // if vr init is finished

    PVector playArea = new PVector();
    HmdQuad_t playAreaRect = new HmdQuad_t();

    int woldScale = 200;
    private String driverPath;

    @Override
    public void settings() {
        size(1280, 720, P3D);
    }

    private static IVRSystem hmd;
    private static IntBuffer errorBuffer = GLBuffers.newDirectIntBuffer(1);

    @Override
    public void setup() {
        frameRate(90);

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
        text("FPS: " + frameRate, width - textWidth("FPS: 90 ") - 10, 20);

        text("PlayArea: " + playArea.x + " m x " + playArea.y + " m", 10, 20);


        for (int i = 0; i <= playAreaRect.vCorners.length; i++) {

            line(width / 2 + playAreaRect.vCorners[i % 4].v[0] * woldScale,
                    height / 2 + playAreaRect.vCorners[i % 4].v[2] * woldScale,
                    width / 2 + playAreaRect.vCorners[(i + 1) % 4].v[0] * woldScale,
                    height / 2 + playAreaRect.vCorners[(i + 1) % 4].v[2] * woldScale);
        }


        getDevices();
        getTrackingPosition();


        drawToHeadset();

        // process events
        VREvent_t event = new VREvent_t();
        while (hmd.PollNextEvent.apply(event, event.size())) {

            switch (event.eventType) {
                //Handle quiting the app from Steam
                case VREvent_DriverRequestedQuit:
                case VREvent_Quit:
                    exit();
                    break;

                case VREvent_ChaperoneDataHasChanged:
                case VREvent_ChaperoneUniverseHasChanged:
                case VREvent_ChaperoneTempDataHasChanged:
                case VREvent_ChaperoneSettingsHaveChanged:
                    getChaperoneData();
                    break;
            }
        }
    }

    private void getDevices() {
        TrackedDevicePose_t[] trackedDevicePose = new TrackedDevicePose_t[k_unMaxTrackedDeviceCount];
        hmd.GetDeviceToAbsoluteTrackingPose.apply(TrackingUniverseStanding, 0, trackedDevicePose, k_unMaxTrackedDeviceCount);//only hmd

        for (int unDevice = 0; unDevice < k_unMaxTrackedDeviceCount; unDevice++) {

            PImage img = icons.get(unDevice);
            if (img != null) {
                image(img, 50, 50 + unDevice * (img.height + 30));
                text(hmd.GetTrackedDevicePropertyString(unDevice, Prop_ModelNumber_String, errorBuffer), 20, 50 + unDevice * (img.height + 30) + img.height);
            }
        }
    }

    private final PGraphicsOpenGL[] views = new PGraphicsOpenGL[2];


    private void drawToHeadset() {
        PJOGL pgl = (PJOGL) beginPGL();
        GL2ES2 gl = pgl.gl.getGL3();

        final Texture eyeTextures[] = {
                new Texture((PGraphicsOpenGL) g),
                new Texture((PGraphicsOpenGL) g)};
//        createGraphics(w, h, P3D);

        endPGL();
        /*

        // opengl texture
// The framebuffer, which regroups 0, 1, or more textures, and 0 or 1 depth buffer.
        IntBuffer FramebufferName = GLBuffers.newDirectIntBuffer(0);
        GL.glGenFramebuffers(1, FramebufferName);
        glBindFramebuffer(GL_FRAMEBUFFER, FramebufferName);


        PGraphics2D.



        Texture_t textureLeft = new Texture_t(, TextureType_OpenGL, ColorSpace_Auto);
        Texture_t textureRight;
        compositor.Submit.apply(LEFT, textureLeft);
        compositor.Submit.apply(RIGHT, textureRight);
*/

        //Set the OpenGL texture geometry
        VRTextureBounds_t GLBounds = new VRTextureBounds_t(0, 1, 1, 0);
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

            driverPath = VR.VR_RuntimePath() + "drivers\\";
            loadIcons();

            getChaperoneData();
        }
        return true;
    }

    HashMap<Integer, PImage> icons = new HashMap<>();

    private void loadIcons() {
        // TODO add to on device connected event
        for (int trackedDevice = k_unTrackedDeviceIndex_Hmd;
             trackedDevice < k_unMaxTrackedDeviceCount;
             trackedDevice++) {
            //If the device is not connected, pass.
            if (!hmd.IsTrackedDeviceConnected.apply(trackedDevice))
                continue;
            System.out.println("--------------------------" + trackedDevice + "-------------------------------------");
            String driver = hmd.GetTrackedDevicePropertyString(trackedDevice, Prop_TrackingSystemName_String, errorBuffer);
            System.out.println(driver);


            System.out.println(hmd.GetTrackedDevicePropertyString(trackedDevice, Prop_NamedIconPathDeviceOff_String, errorBuffer));
            System.out.println(hmd.GetTrackedDevicePropertyString(trackedDevice, Prop_NamedIconPathDeviceSearching_String, errorBuffer));
            System.out.println(hmd.GetTrackedDevicePropertyString(trackedDevice, Prop_NamedIconPathDeviceSearchingAlert_String, errorBuffer));
            String ready = hmd.GetTrackedDevicePropertyString(trackedDevice, Prop_NamedIconPathDeviceReady_String, errorBuffer);

            icons.put(trackedDevice, loadImage(driverPath + driver + "\\resources" + ready.replaceAll("\\{\\w*}", "")));
            System.out.println(ready);
            System.out.println(hmd.GetTrackedDevicePropertyString(trackedDevice, Prop_NamedIconPathDeviceReadyAlert_String, errorBuffer));
            System.out.println(hmd.GetTrackedDevicePropertyString(trackedDevice, Prop_NamedIconPathDeviceNotReady_String, errorBuffer));
            System.out.println(hmd.GetTrackedDevicePropertyString(trackedDevice, Prop_NamedIconPathDeviceStandby_String, errorBuffer));
            System.out.println(hmd.GetTrackedDevicePropertyString(trackedDevice, Prop_NamedIconPathDeviceAlertLow_String, errorBuffer));
        }
    }

    private void getChaperoneData() {
        System.out.println("Getting Chaperone data");
        FloatBuffer w = GLBuffers.newDirectFloatBuffer(1);
        FloatBuffer h = GLBuffers.newDirectFloatBuffer(1);
        chaperone.GetPlayAreaSize.apply(w, h);

        playArea.set(w.get(0), h.get(0));

        chaperone.GetPlayAreaRect.apply(playAreaRect);

    }

    @Override
    protected void finalize() throws Throwable {
        VR.VR_Shutdown();

    }

    public static void main(String[] args) {
        PApplet.main(Status.class);
    }

    /**
     * Converts {@link HmdMatrix34_t} to {@link PMatrix3D}
     *
     * @param mat OpenVR matrix
     * @return Processing matrix
     */
    public static PMatrix3D GetMatrix(HmdMatrix34_t mat) {
        return new PMatrix3D(
                mat.m[0], mat.m[1], mat.m[2], mat.m[3],
                mat.m[4], mat.m[5], mat.m[6], mat.m[7],
                mat.m[8], mat.m[9], mat.m[10], mat.m[11],
                0, 0, 0, 0
        );
    }

    /**
     * Get the vector representing the position
     */
    public static PVector GetPosition(HmdMatrix34_t matrix) {
        PVector vector = new PVector();

        vector.x = matrix.get(0, 3);
        vector.y = matrix.get(1, 3);
        vector.z = matrix.get(2, 3);

        return vector;
    }

    /**
     * https://github.com/Omnifinity/OpenVR-Tracking-Example/blob/master/HTC%20Lighthouse%20Tracking%20Example/LighthouseTracking.cpp
     */
    public void getTrackingPosition() {

        TrackedDevicePose_t[] trackedDevicePose = new TrackedDevicePose_t[k_unMaxTrackedDeviceCount];
        hmd.GetDeviceToAbsoluteTrackingPose.apply(TrackingUniverseStanding, 0, trackedDevicePose, k_unMaxTrackedDeviceCount);//only hmd

        for (int unDevice = 0; unDevice < k_unMaxTrackedDeviceCount; unDevice++) {
            // if not connected just skip the rest of the routine
            if (!hmd.IsTrackedDeviceConnected.apply(unDevice))
                continue;


            if (hmd.IsInputFocusCapturedByAnotherProcess.apply()) {
                System.out.println("Input Focus by Another Process");
            }

            HmdMatrix34_t mat = null;

            // Get what type of device it is and work with its data
            int trackedDeviceClass = hmd.GetTrackedDeviceClass.apply(unDevice);
            switch (trackedDeviceClass) {
                case VR.ETrackedDeviceClass.TrackedDeviceClass_HMD:
                    // print stuff for the HMD here, see controller stuff in next case block

                    // get pose relative to the safe bounds defined by the user

                    mat = trackedDevicePose[unDevice].mDeviceToAbsoluteTracking;
                    break;

                default:
                case TrackedDeviceClass_Controller:
                    VRControllerState_t controllerState = new VRControllerState_t();
                    TrackedDevicePose_t pose = new TrackedDevicePose_t();
                    hmd.GetControllerStateWithPose.apply(TrackingUniverseStanding, unDevice, controllerState, controllerState.size(), pose);
                    mat = pose.mDeviceToAbsoluteTracking;

                    break;
            }

            if (mat != null) {
                PVector pos = GetPosition(mat);
                PMatrix3D rot = GetMatrix(mat);
                float angle = (float) Math.atan2(-rot.m20, Math.sqrt(rot.m21 * rot.m21 + rot.m22 * rot.m22));

                point(width / 2 + pos.x * woldScale, height / 2 + pos.z * woldScale); // hmd working

                pushMatrix();

                translate(width / 2 + pos.x * woldScale, height / 2 + pos.z * woldScale);
                rotateZ(-angle);

                PImage img = icons.get(unDevice);
                imageMode(CENTER);
                image(img, 0, 0);

                popMatrix();
            }
        }
    }
}