package zebra.BarcodeDetector;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class OverlayView extends View {

    private static final String TAG = "OverlayView";

    // GOOD RESOLUTIONS FOR BARCODES, 2FT AWAY: 1080x1920 (2Mpx) less accurate,
    // 2048x1536 (3Mpx), 1920x2560 (5Mpx) slower
    public static int CAMERA_RESOLUTION_WIDTH = 480;
    public static int CAMERA_RESOLUTION_HEIGHT = 640;
    public static int CHOSEN_SCENE = 0;
    public static double ZOOM_RATIO = 1.0;
    public static String BARCODETOHIGHLIGHT = "";

    public final ConcurrentLinkedDeque<BCEvent> clq = new ConcurrentLinkedDeque<>();
    public final Set<String> performanceSet = ConcurrentHashMap.newKeySet();
    public final Set<String> highlightSet = ConcurrentHashMap.newKeySet();
    public int readRate = 0;

    public final Paint paintYellow;
    public final Paint paintGray;
    public final Paint paintGreen;
    public final Paint paintRed;
    public final Paint ink;

    public OverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        paintYellow = new Paint();
        paintYellow.setColor(Color.YELLOW);
        paintYellow.setStrokeWidth(5f);
        paintYellow.setStyle(Paint.Style.STROKE);

        paintGray = new Paint();
        paintGray.setColor(Color.GRAY);
        paintGray.setStrokeWidth(5f);
        paintGray.setStyle(Paint.Style.STROKE);

        paintGreen = new Paint();
        paintGreen.setColor(Color.rgb(0, 190, 0));
        paintGreen.setStrokeWidth(20f);
        paintGreen.setStyle(Paint.Style.STROKE);

        paintRed = new Paint();
        paintRed.setColor(Color.rgb(190, 0, 0));
        paintRed.setStrokeWidth(20f);
        paintRed.setStyle(Paint.Style.FILL_AND_STROKE);

        ink = new Paint();
        ink.setColor(Color.YELLOW);
        ink.setTextSize(50f);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        long timestamp = System.currentTimeMillis();

        if (!clq.isEmpty()) {
            Iterator<BCEvent> it = clq.iterator();
            while (it.hasNext()) {
                BCEvent ev = it.next();
                if (timestamp - ev.timestamp < 500) {
                    float XX = ev.xavg;
                    float YY = ev.yavg;
                    canvas.drawCircle(XX, YY, 20f, ev.paint);
                    canvas.drawText(ev.bcValue + "-[" + ev.trackingID + "]", XX, YY, ink);
                } else {
                    it.remove();
                }
            }
        }
        Log.d("drawCircle", "clq size = " + clq.size());
    }

    public float[] rotateAndScaleCoordinates(
            float cameraX, float cameraY,
            float cameraWidth, float cameraHeight,
            float screenWidth, float screenHeight) {
        float scaleX = screenWidth / cameraWidth;
        float scaleY = screenHeight / cameraHeight;
        return new float[]{(cameraWidth - cameraY) * scaleX, cameraX * scaleY};
    }
}
