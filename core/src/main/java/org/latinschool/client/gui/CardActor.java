
package org.latinschool.client.gui;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public class CardActor extends Image {
    private TextureRegion cardRegion;

    public CardActor(TextureRegion cardRegion) {
        super(new TextureRegionDrawable(cardRegion));
        this.cardRegion = cardRegion;
        setSize(88 * 0.8f, 124 * 0.8f);
    }

    public void setCard(TextureRegion newRegion) {
        this.cardRegion = newRegion;
        setDrawable(new TextureRegionDrawable(newRegion));
    }

    public TextureRegion getCard() { return cardRegion; }
}
