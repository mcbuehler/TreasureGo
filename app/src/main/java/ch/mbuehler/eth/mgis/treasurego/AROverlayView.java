package ch.mbuehler.eth.mgis.treasurego;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.location.Location;
import android.opengl.Matrix;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.Set;

/**
 * Created by vanagnos on 03.04.2017. Updated by goebelf on 18.04.2018.
 * We extend the View Class of Android (see also: https://developer.android.com/reference/android/view/View.html)
 * A View occupies a rectangular area on the screen and is responsible for drawing and event handling.
 * Here we override the onDraw function and we also draw the ARPoints we want. See the onDraw()
 * function and how we transform the GPS84 coordinates to camera coordinates.
 */

public class AROverlayView extends View {

    Context context;
    private float[] rotatedProjectionMatrix = new float[16];
    private Location currentLocation;
    private Set<ARGem> arGems;
    private Toast toast;

    /**
     * Constructor of the AROverlyView class. Takes the contex and List of ARPoints as arguments.
     *
     * @param context the context creating the class
     * @param arGems  the List of ARPoints to be drawn
     */
    public AROverlayView(Context context, Set<ARGem> arGems) {
        super(context);

        this.context = context;
        this.arGems = arGems;

        toast = Toast.makeText(context, "", Toast.LENGTH_SHORT);
    }

    /**
     * Whenever the orientation of the phone changes, this method should be called to force the View to be redrawn.
     *
     * @param rotatedProjectionMatrix the new projectionMatrix
     */
    public void updateRotatedProjectionMatrix(float[] rotatedProjectionMatrix) {
        this.rotatedProjectionMatrix = rotatedProjectionMatrix;

        // Here we force the View to be redrawn. So each time we update the projection matrix
        // the view is redrawn
        this.invalidate();
    }

    /**
     * Whenever the location changes, this method should be called to force the View to be redrawn.
     *
     * @param currentLocation the new location
     */
    public void updateCurrentLocation(Location currentLocation) {
        this.currentLocation = currentLocation;

        // Here we force the View to be redrawn. So each time we update the projection matrix
        // the view is redrawn
        this.invalidate();
    }

    /**
     * Called when the view should render its content. If the current Location is valid, we
     * calculate the positions of each of the ARGem corresponding to the user's current position
     * and draw them.
     *
     * @param canvas the canvas on which the background will be drawn
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // if the current location is invalid
        if (currentLocation == null) {
            // do nothing
            return;
        }

        // variables for the point representation
        final int radius = 30;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setTextSize(60);

        // Transform the ARPoints coordinates from WGS84 to camera coordinates
        for (ARGem arGem : arGems) {

            // First we transform from GPS coordinates to ECEF coordinates and then to Navigation Coordinates
            float[] currentLocationInECEF = CoordinateTransformator.WSG84toECEF(currentLocation);
            float[] pointInECEF = CoordinateTransformator.WSG84toECEF(arGem.getLocation());
            float[] pointInENU = CoordinateTransformator.ECEFtoENU(currentLocation, currentLocationInECEF, pointInECEF);

            // Afterwards we transform the Navigation coordinates (ENU) to Camera coordinates
            float[] cameraCoordinateVector = new float[4];

            // To convert ENU coordinate to Camera coordinate, we will multiply camera projection matrix
            // with ENU coordinate vector, the result is a vector [v0, v1, v2, v3].
            Matrix.multiplyMV(cameraCoordinateVector, 0, rotatedProjectionMatrix, 0, pointInENU, 0);


            // cameraCoordinateVector[2] is z, that always less than 0 to display on right position
            // if z > 0, the point will display on the opposite
            if (cameraCoordinateVector[2] < 0) {

                //Then x = (0.5 + v0 / v3) * widthOfCameraView and y = (0.5 - v1 / v3) * heightOfCameraView.
                float x = (0.5f + cameraCoordinateVector[0] / cameraCoordinateVector[3]) * canvas.getWidth();
                float y = (0.5f - cameraCoordinateVector[1] / cameraCoordinateVector[3]) * canvas.getHeight();

                arGem.x = x;
                arGem.y = y;

                canvas.drawCircle(x, y, radius, paint);
                canvas.drawText(arGem.getName(), x - (30 * arGem.getName().length() / 2), y - 80, paint);
            }
        }
    }

    public OnTouchListener getOnTouchListener(final String targetTreasureUUID) {
        return new OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                double DIST_THRESHOLD = 60;

                double x = motionEvent.getX();
                double y = motionEvent.getY();
                Log.v("TOUCH", String.format("%f, %f", x, y));

                double closestDistance = 99999999;
                ARGem closestPoint = null;
                for (ARGem arGem : arGems) {
                    double distance = arGem.euclideanDistanceTo(x, y);
                    if (closestPoint == null || distance < closestDistance) {
                        closestPoint = arGem;
                        closestDistance = distance;
                    }
                }

                if (closestDistance < DIST_THRESHOLD) {
                    arGems.remove(closestPoint);
                    String info = closestPoint.getName() + " " + context.getString(R.string.collected);
                    toast.setText(info);
                    toast.show();

                    if (arGems.isEmpty()) {
                        //            // Go to next Activity
                        Intent intent = new Intent(context, TreasureFoundActivity.class);
                        intent.putExtra(MainActivity.TREASURE_KEY, targetTreasureUUID);
                        context.startActivity(intent);
                        return true;
                    }
                } else {
                    toast.setText(R.string.noGemFoundHere);
                    toast.show();
                }


                return true;
            }
        };
    }
}
