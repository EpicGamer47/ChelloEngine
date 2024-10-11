package storyInProcessingChris;

import processing.core.PApplet;

public class Main extends PApplet {
	private StoryRunner sr;
	
	public static void main(String[] args) {
		PApplet.main("storyInProcessingChris.Main");
	}
	
	@Override
	public void settings() {
		size(1600, 900, JAVA2D);
	}

	@Override
	public void setup() {
		sr = new StoryRunner(this, "./data/qTest", "./data/shared");
		frameRate(30);
	}
	
	@Override
	public void draw() {
//		try {
//			sr.draw();
//		} 
//		catch (NumberFormatException | NullPointerException e) {
//			e.printStackTrace();
//		}
			
		sr.draw();
		println(frameRate);
	}
	
	@Override
	public void mouseClicked() {
		sr.mouseClicked();
	}
	
	@Override
	public void keyPressed() {
		sr.keyPressed(key, keyCode);
	}
	
	public void updateStoryRunner(StoryRunner sr) {
		this.sr = sr;
	}
}
