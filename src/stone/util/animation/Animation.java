package stone.util.animation;

public class Animation {
    private Easing easing;
    private long duration;
    private long millis;
    private long startTime;
    private double startValue;
    private double destinationValue;
    private double value;
    private boolean finished;

    public Animation(Easing easing, long duration) {
        this.easing = easing;
        this.startTime = System.currentTimeMillis();
        this.duration = duration;
    }

    public void run(double destination) {
        this.millis = System.currentTimeMillis();
        if (this.destinationValue != destination) {
            this.destinationValue = destination;
            this.reset();
        } else {
            this.finished = this.millis - this.duration >= this.startTime;
            if (this.finished) {
                this.value = destination;
                return;
            }
        }

        if (this.duration <= 0) {
            this.value = destination;
            this.finished = true;
            return;
        }

        double progress = (double) (this.millis - this.startTime) / (double) this.duration;
        double clampedProgress = Math.max(0.0, Math.min(1.0, progress));
        double factor = this.easing.getFunction().apply(clampedProgress);
        this.value = this.startValue + (destination - this.startValue) * factor;
        if (clampedProgress >= 1.0) {
            this.finished = true;
        }
    }

    public void Q(double destination) {
        run(destination);
    }

    public void reset() {
        this.startTime = System.currentTimeMillis();
        this.startValue = this.value;
        this.finished = false;
    }

    public Easing getEasing() {
        return easing;
    }

    public void setEasing(Easing easing) {
        this.easing = easing;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public double getDestinationValue() {
        return destinationValue;
    }

    public void setDestinationValue(double destinationValue) {
        this.destinationValue = destinationValue;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }
}
