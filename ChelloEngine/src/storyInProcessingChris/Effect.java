package storyInProcessingChris;

import static processing.core.PApplet.*;
import static storyInProcessingChris.EffectType.*;

import java.awt.geom.Point2D;
import processing.core.PApplet;
import processing.core.PImage;

public class Effect {
	protected StoryRunner sr;
	protected PApplet parent;
	protected Character ch;
	protected float startTime;
	protected int time;
	
	private EffectType type;
	private String[] args;
	
	// NOTE FOR DEBUGGING: VERY IMPORTANT
	// SET ONE TIME EFFECTS TO "time = 2"
	public Effect(StoryRunner sr, Character ch, String type, int time, String[] args) {
		this.sr = sr;
		this.parent = sr.getParent();
		this.ch = ch;
		this.startTime = time;
		this.type = EffectType.valueOf(type.toUpperCase());
		this.time = time;
		this.args = args;
	}
	
	public void update() {
		time--;
	}
	
	public boolean isExpired() {
		return time <= 0 && !type.equals(LOOP);
	}
	
	public void onExpiry() {
		switch (type) {
		case FADE:
			ch.setEmotion(args[0]);
		default:
			break;
		}
	}
	
	public void draw(float x, float y) {
		switch (type) {
		case BOOM:
			boom(x, y); break;
		case FADE:
			fade(x, y); break;
		case SIZE:
			size(); break;
		case SCALE:
			scale(); break;
		case LOOP:
			loop(); break;
//		default:
//			throw new IllegalArgumentException(type + " is not a valid function for an effect");
		}
	}

	// boom(float endScaleFactor, String image)
	// boom(float endScaleFactor, String image, int alphaOffset)
	private void boom(float x, float y) {
		float progress = (float) Math.sqrt((startTime - time) / startTime);
		
		float sf = 1 + (Float.parseFloat(args[0]) - 1) * progress; // scale factor
		PImage image = (PImage) sr.getFile("image", args[1]);
		int aO = args.length > 2 ? Integer.parseInt(args[2], 16) : 0;
		
		parent.tint(255, (255 - aO) * (1 - progress));
		parent.imageMode(CENTER);
		Character.drawChar(image, x, y, ch, new Point2D.Float(sf, sf));
		parent.tint(255);
	}
	
	// loop(String type, int interval, String[] args)
	private void loop() {
		// TODO not really needed right now
	}
	
	// scale(float uniformScaleFactor)
	// scale(float xScaleFactor, yScaleFactor)
	private void scale() {
		Point2D.Float s = ch.getScale();
		float arg1 = Float.parseFloat(args[0]);
		
		if (args.length == 1) {
			ch.getScale().setLocation(s.x + arg1, s.y + arg1);
		}
		else {
			float arg2 = Float.parseFloat(args[1]);
			ch.getScale().setLocation(s.x + arg1, s.y + arg2);
		}		
	}
	
	// size(int x, int y)
	private void size() {
		ch.getSize().setLocation(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
		ch.getScale().setLocation(1, 1);
	}
	
	// Precondition: endAlpha > startAlpha
	// boom(String emotion, int startAlpha, int endAlpha)
	private void fade(float x, float y) {
		float progress = (startTime - time) / startTime;
		
		PImage image = (PImage) sr.getFile("image", ch.getEmotions().get(args[0]));
		int a0 = args.length > 2 ? Integer.parseInt(args[1], 16) : 0;
		int a1 = args.length > 2 ? Integer.parseInt(args[2], 16) : 0;
		
		parent.tint(255, (a1 - a0) * (progress));
		parent.imageMode(CENTER);
		parent.image(image, x, y, ch.getSize().x * ch.getScale().x, ch.getSize().y * ch.getScale().y);
		Character.drawChar(image, x, y, ch);
		parent.tint(255);
	}
}
