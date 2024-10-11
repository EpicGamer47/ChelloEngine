package storyInProcessingChris;

import static processing.core.PApplet.*;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.Iterator;
import processing.core.PApplet;
import processing.core.PImage;

public class Character implements Comparable<Character> {
	private PApplet parent;
	private StoryRunner sr;
	private TreeMap<String, String> emotions;
	private ArrayList<Effect> effects;
	
	private String currentEmotion;
	private Point2D.Float size;
	private Point2D.Float scale;
	private String name;
	private PImage currentSprite;

	public Character(StoryRunner sr, String[][] script, String name) {
		this.parent = sr.getParent();
		this.sr = sr;
		this.name = name;
		emotions = new TreeMap<String, String>();
		effects = new ArrayList<Effect>();
		
		size = new Point2D.Float();
		scale = new Point2D.Float(1, 1);
		
		currentEmotion = null;
		
		for (String[] line : script) {
			execute(line);
		}
	}
	
	public Character(String name) { // empty by design, used for searches
		this.name = name;
	}

	private void execute(String[] line) {
		switch (line[0]) {
		case "emotion":
			emotions.put(line[1], line[2]);
			sr.loadFile("image", line[2]); 
			break;
		case "size":
			size.x = Integer.parseInt(line[1]);
			size.y = Integer.parseInt(line[2]);
			break;
		default:
			throw new IllegalArgumentException(line[0] + " is not a valid function for a character");
		}
	}
	
	public void draw(float x, int y) {
		parent.imageMode(CENTER);
		drawChar(currentSprite, x, y, this);
		
		if (effects.size() == 0) return;
		
		Iterator<Effect> it = effects.iterator();
		while (it.hasNext()) {
			Effect e = it.next();
			e.update();
			e.draw(x, y);
			
			if (e.isExpired()) {
				e.onExpiry();
				it.remove();
			}
		}
	}
	
	public TreeMap<String, String> getEmotions() {
		return emotions;
	}
	
	public Point2D.Float getSize() {
		return size;
	}
	
	public Point2D.Float getScale() {
		return scale;
	}
	
	public String getName() {
		return name;
	}
	
	public void setEmotion(String emotion) {
		if (emotions.containsKey(emotion)) {
			currentEmotion = emotion;
			currentSprite = (PImage) sr.getFile("image", emotions.get(emotion));
		}
		else {
			throw new IllegalArgumentException(emotion + " is not an emotion for " + name);
		}
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o)
	        return true;
		if (o == null)
	        return false;
		if (getClass() != o.getClass())
	        return false;

		Character e = (Character) o;
		return this.name.equals(e.getName());
	}
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}

	public void addEffect(String[] args) {
		String name = args[0];
		int time = Integer.parseInt(args[1]);
		String[] args2 = Arrays.copyOfRange(args, 2, args.length);
		
		effects.add(new Effect(sr, this, name, time, args2));
	}

	@Override
	public int compareTo(Character o) {
		return name.compareTo(o.name);
	}
	
	public ArrayList<Effect> getEffects() {
		return effects;
	}
	
	public static void drawChar(PImage img, float x, float y, Character ch) {
		ch.parent.image(img, x, y, ch.getSize().x * ch.getScale().x, ch.getSize().y * ch.getScale().y);
	}
	
	public static void drawChar(PImage img, float x, float y, Character ch, Point2D.Float sf) {
		ch.parent.image(img, x, y, ch.getSize().x * ch.getScale().x * sf.x, ch.getSize().y * ch.getScale().y * sf.y);
	}
}
