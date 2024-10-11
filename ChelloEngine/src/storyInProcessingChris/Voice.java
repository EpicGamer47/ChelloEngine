package storyInProcessingChris;

import static storyInProcessingChris.VoiceType.*;

import processing.core.PApplet;
import processing.sound.SoundFile;

public class Voice {
//	private PApplet parent;
	private StoryRunner sr;
	
	private VoiceType type;
	private String[] args;
	
	public Voice(StoryRunner sr, String type, String[] args) {
		this.sr = sr;
//		this.parent = sr.getParent();
		this.type = VoiceType.valueOf(type.toUpperCase());
		this.args = args;
	}
	
	public void play() {
		switch(type) {
		case TONE:
			((SoundFile) sr.getFile("sound", args[0])).loop();
			break;
		case LINE:
			((SoundFile) sr.getFile("sound", args[0])).play();
			break;
		case BINDING: // debug - recursive call
			sr.getVoiceCache().get(args[0]).play();
			break;
		case BLANK:
			break;
		}
	}
	
	public void end() {
		switch(type) {
		case TONE: 
		case LINE:
			((SoundFile) sr.getFile("sound", args[0])).stop();
			break;
		case BINDING: // debug - recursive call
			sr.getVoiceCache().get(args[0]).end();
			break;
		case BLANK:
			break;
		}
	}
}
