package placeme.ru.placemedemo.core;

import android.app.Activity;
import android.location.Location;

import actions.Action;
import actions.ActionCalcRelativePos;
import actions.ActionMoveCameraBuffered;
import actions.ActionRotateCameraBuffered;
import actions.ActionWASDMovement;
import actions.ActionWaitForAccuracy;
import commands.Command;
import commands.gl.CommandCameraMoveAndLookAt;
import geo.GeoObj;
import gl.CustomGLSurfaceView;
import gl.GL1Renderer;
import gl.GLCamera;
import gl.GLFactory;
import gui.GuiSetup;
import system.EventManager;
import system.Setup;
import util.Log;
import util.Vec;
import worldData.SystemUpdater;
import worldData.World;

public abstract class ArSetup extends Setup {

    protected static final int ZDELTA = 5;
    private static final String LOG_TAG = "DefaultARSetup";

    public GLCamera camera;
    public World world;
    private ActionWASDMovement wasdAction;
    private GL1Renderer myRenderer;
    private boolean addObjCalledOneTieme;
    private ActionWaitForAccuracy minAccuracyAction;
    private Action rotateGLCameraAction;

    public ArSetup() {

    }

    @Override
    public void _a_initFieldsIfNecessary() {
        camera = new GLCamera(new Vec(0, 0, 2));
        world = new World(camera);
    }

    public World getWorld() {
        return world;
    }

    public GLCamera getCamera() {
        return camera;
    }

    /**
     * This will be called when the GPS accuracy is high enough
     *
     * @param renderer
     * @param world
     * @param objectFactory
     */
    public abstract void addObjectsTo(GL1Renderer renderer, World world,
                                      GLFactory objectFactory);

    @Override
    public void _b_addWorldsToRenderer(GL1Renderer renderer,
                                       GLFactory objectFactory, GeoObj currentPosition) {
        myRenderer = renderer;
        renderer.addRenderElement(world);
    }

    @Override
    public void _c_addActionsToEvents(final EventManager eventManager,
                                      CustomGLSurfaceView arView, SystemUpdater updater) {
        wasdAction = new ActionWASDMovement(camera, 25, 50, 20);
        rotateGLCameraAction = new ActionRotateCameraBuffered(camera);
        eventManager.addOnOrientationChangedAction(rotateGLCameraAction);

        arView.addOnTouchMoveListener(wasdAction);
        // eventManager.addOnOrientationChangedAction(rotateGLCameraAction);
        eventManager.addOnTrackballAction(new ActionMoveCameraBuffered(camera,
                5, 25));
        eventManager.addOnLocationChangedAction(new ActionCalcRelativePos(
                world, camera));
        minAccuracyAction = new ActionWaitForAccuracy(getActivity(), 24.0f, 10) {
            @Override
            public void minAccuracyReachedFirstTime(Location l,
                                                    ActionWaitForAccuracy a) {
                callAddObjectsToWorldIfNotCalledAlready();
                if (!eventManager.getOnLocationChangedAction().remove(a)) {
                    Log.e(LOG_TAG,
                            "Could not remove minAccuracyAction from the onLocationChangedAction list");
                }
            }
        };
        eventManager.addOnLocationChangedAction(minAccuracyAction);
    }

    protected void callAddObjectsToWorldIfNotCalledAlready() {
        if (!addObjCalledOneTieme) {
            addObjectsTo(myRenderer, world, GLFactory.getInstance());
        } else {
            Log.w(LOG_TAG, "callAddObjectsToWorldIfNotCalledAlready() "
                    + "called more then one time!");
        }
        addObjCalledOneTieme = true;
    }

    @Override
    public void _d_addElementsToUpdateThread(SystemUpdater updater) {
        updater.addObjectToUpdateCycle(world);
        updater.addObjectToUpdateCycle(wasdAction);
        updater.addObjectToUpdateCycle(rotateGLCameraAction);
    }

    @Override
    public void _e2_addElementsToGuiSetup(GuiSetup guiSetup, final Activity activity) {
        guiSetup.setRightViewAllignBottom();

        guiSetup.addViewToTop(minAccuracyAction.getView());

        guiSetup.addImangeButtonToRightView(android.R.drawable.arrow_up_float,
                new Command() {
                    @Override
                    public boolean execute() {
                        camera.changeZPositionBuffered(+ZDELTA);
                        return false;
                    }
                });
        guiSetup.addImangeButtonToRightView(android.R.drawable.arrow_down_float,
                new Command() {
                    @Override
                    public boolean execute() {
                        camera.changeZPositionBuffered(-ZDELTA);
                        return false;
                    }
                });

        guiSetup.addButtonToBottomView(new CommandCameraMoveAndLookAt(camera, new Vec(0, 0, 40)), "God Cam");
    }
}