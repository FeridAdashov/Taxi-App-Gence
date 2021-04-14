package texel.ChangedOSMLibraries;


import org.oscim.renderer.BucketRenderer;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.bucket.RenderBucket;
import org.oscim.renderer.bucket.TextureBucket;

class TextRenderer extends BucketRenderer {
    static final Class<TextRenderer> log = TextRenderer.class;
    static final boolean dbg = false;

    private final LabelLayer.Worker mWorker;
    long lastDraw = 0;

    public TextRenderer(LabelLayer.Worker worker) {
        mWorker = worker;
    }

    @Override
    public synchronized void update(GLViewport v) {

        LabelTask t;
        synchronized (mWorker) {
            t = mWorker.poll();
            if (t == null) {
                if (!mWorker.isRunning()) {
                    mWorker.submit(50);
                }
                return;
            }
            buckets.clear();
        }

        // set new TextLayer to be uploaded and rendered
        buckets.set(t.layers);
        mMapPosition = t.pos;
        compile();
    }

    @Override
    public synchronized void render(GLViewport v) {
        GLState.test(false, false);
        //Debug.draw(pos, layers);

        buckets.vbo.bind();

        float scale = (float) (v.pos.scale / mMapPosition.scale);

        setMatrix(v, false);

        for (RenderBucket l = buckets.get(); l != null; )
            l = TextureBucket.Renderer.draw(l, v, scale);
    }

}
