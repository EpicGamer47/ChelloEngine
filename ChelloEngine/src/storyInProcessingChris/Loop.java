package storyInProcessingChris;

import java.util.ArrayDeque;

public class Loop extends Effect {
	ArrayDeque<Effect> dq;

	public Loop(StoryRunner sr, Character ch, String type, int time, String[] args) {
		super(sr, ch, type, time, args);
		
		dq = new ArrayDeque<Effect>();
	}

}
