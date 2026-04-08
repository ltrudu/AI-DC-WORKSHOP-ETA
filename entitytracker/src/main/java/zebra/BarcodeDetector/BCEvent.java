package zebra.BarcodeDetector;

import android.graphics.Paint;

public class BCEvent {
    public final float xavg;
    public final float yavg;
    public final Paint paint;
    public final String bcValue;
    public final String trackingID;
    public final long timestamp;

    public BCEvent(float xavg, float yavg, Paint paint, String bcValue, String trackingID, long timestamp) {
        this.xavg = xavg;
        this.yavg = yavg;
        this.paint = paint;
        this.bcValue = bcValue;
        this.trackingID = trackingID;
        this.timestamp = timestamp;
    }
}
