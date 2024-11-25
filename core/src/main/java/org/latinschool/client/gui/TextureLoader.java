
package org.latinschool.client.gui;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import org.latinschool.shared.Card;
import org.latinschool.shared.Rank;
import org.latinschool.shared.Suit;

import java.util.HashMap;
import java.util.Map;

public class TextureLoader {
    private Map<String, TextureRegion> cardTextures;
    private TextureRegion backTexture;

    public TextureLoader() {
        cardTextures = new HashMap<>();
        loadCardTextures();
    }

    private void loadCardTextures() {
        loadSuit("Spades");
        loadSuit("Hearts");
        loadSuit("Diamonds");
        loadSuit("Clubs");

        try {
            Texture back = new Texture("pokerpack/Cards/back.png");
            backTexture = new TextureRegion(back, 0, 0, 88, 124);
            System.out.println("Loaded card back texture successfully.");
        } catch (Exception e) {
            System.err.println("Error loading card back texture: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadSuit(String suitName) {
        try {
            Texture suitTexture = new Texture("pokerpack/Cards/" + suitName + ".png");
            TextureRegion[][] regions = TextureRegion.split(suitTexture, 88, 124);

            for (int i = 0; i < 5; i++) {
                Rank rank = Rank.values()[i];
                Card card = new Card(Suit.valueOf(suitName.toUpperCase()), rank);
                cardTextures.put(card.toString(), regions[0][i]);
            }

            for (int i = 0; i < 5; i++) {
                Rank rank = Rank.values()[5 + i];
                Card card = new Card(Suit.valueOf(suitName.toUpperCase()), rank);
                cardTextures.put(card.toString(), regions[1][i]);
            }

            for (int i = 0; i < 3; i++) {
                Rank rank = Rank.values()[10 + i];
                Card card = new Card(Suit.valueOf(suitName.toUpperCase()), rank);
                cardTextures.put(card.toString(), regions[2][i]);
            }

            System.out.println("Loaded " + suitName + " textures successfully.");
        } catch (Exception e) {
            System.err.println("Error loading " + suitName + " textures: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public TextureRegion getTextureRegion(Card card) {
        TextureRegion region = cardTextures.get(card.toString());
        if (region == null) {
            System.err.println("Texture not found for card: " + card.toString());
        }
        return region;
    }

    public TextureRegion getCardBack() {
        return backTexture;
    }
}
