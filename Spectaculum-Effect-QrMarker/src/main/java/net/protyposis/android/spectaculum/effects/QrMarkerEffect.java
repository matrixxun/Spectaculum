/*
 * Copyright 2014 Mario Guggenberger <mg@protyposis.net>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.protyposis.android.spectaculum.effects;

import net.protyposis.android.spectaculum.gles.Framebuffer;
import net.protyposis.android.spectaculum.gles.Texture2D;
import net.protyposis.android.spectaculum.gles.TexturedRectangle;
import net.protyposis.android.spectaculum.gles.qrmarker.CannyShaderProgram;
import net.protyposis.android.spectaculum.gles.qrmarker.ConsenseShaderProgram;
import net.protyposis.android.spectaculum.gles.qrmarker.GaussShaderProgram;
import net.protyposis.android.spectaculum.gles.qrmarker.GradientShaderProgram;
import net.protyposis.android.spectaculum.gles.qrmarker.QrResponseShaderProgram;

/**
 * Created by Mario on 07.09.2014.
 */
public class QrMarkerEffect extends BaseEffect {

    private GaussShaderProgram mGaussShader;
    private GradientShaderProgram mGradientShader;
    private CannyShaderProgram mCannyShader;
    private QrResponseShaderProgram mQrResponseShader;
    private ConsenseShaderProgram mConsensusShader;

    private Framebuffer mFramebuffer1;
    private Framebuffer mFramebuffer2;

    private TexturedRectangle mTexturedRectangle;

    private CannyEdgeEffect mCannyEdgeEffect;

    public QrMarkerEffect() {
        mCannyEdgeEffect = new CannyEdgeEffect();
    }

    @Override
    public void init(int width, int height) {
        // PART OF THE UGLY HACK described in setTextureSizeHack
        // Cannot call it on base class QrMarkerShaderProgram because it is hidden outside its package
        GaussShaderProgram.setTextureSizeHack(width, height);

        mGaussShader = new GaussShaderProgram();
        mGaussShader.setTextureSize(width, height);

        mGradientShader = new GradientShaderProgram();
        mGradientShader.setTextureSize(width, height);

        mCannyShader = new CannyShaderProgram();
        mCannyShader.setTextureSize(width, height);

        mQrResponseShader = new QrResponseShaderProgram();
        mQrResponseShader.setTextureSize(width, height);

        mConsensusShader = new ConsenseShaderProgram();
        mConsensusShader.setTextureSize(width, height);

        mFramebuffer1 = new Framebuffer(width, height);
        mFramebuffer2 = new Framebuffer(width, height);

        mTexturedRectangle = new TexturedRectangle();
        mTexturedRectangle.reset();

        setInitialized();
    }

    @Override
    public void apply(Texture2D source, Framebuffer target) {
        applyCannyEdge(source, mFramebuffer1);

        mFramebuffer2.bind();
        mQrResponseShader.use();
        mQrResponseShader.setTexture(mFramebuffer1.getTexture());
        mTexturedRectangle.draw(mQrResponseShader);

        target.bind();
        mConsensusShader.use();
        mConsensusShader.setTexture(mFramebuffer2.getTexture());
        mTexturedRectangle.draw(mConsensusShader);
    }

    private void applyCannyEdge(Texture2D source, Framebuffer target) {
        mFramebuffer1.bind();
        mGaussShader.use();
        mGaussShader.setTexture(source);
        mTexturedRectangle.draw(mGaussShader);

        mFramebuffer2.bind();
        mGradientShader.use();
        mGradientShader.setTexture(mFramebuffer1.getTexture());
        mTexturedRectangle.draw(mGradientShader);

        target.bind();
        mCannyShader.use();
        mCannyShader.setTexture(mFramebuffer2.getTexture());
        mTexturedRectangle.draw(mCannyShader);
    }

    public CannyEdgeEffect getCannyEdgeEffect() {
        return mCannyEdgeEffect;
    }

    /**
     * The CannyEdge Effect is a subeffect of the QrMarker Effect, it is therefore more efficient
     * to share the resources and reuse a common cannyedge subroutine than to instantiate it as
     * a separate effect. If one of the two effects is needed, the other comes with it for free.
     */
    public class CannyEdgeEffect extends BaseEffect {

        @Override
        public void init(int width, int height) {
            if(!QrMarkerEffect.this.isInitialized()) {
                QrMarkerEffect.this.init(width, height);
            }
        }

        @Override
        public void apply(Texture2D source, Framebuffer target) {
            applyCannyEdge(source, target);
        }
    }
}
