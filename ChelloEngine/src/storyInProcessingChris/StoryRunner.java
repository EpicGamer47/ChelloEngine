package storyInProcessingChris;

import static processing.core.PApplet.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.Iterator;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;
import processing.sound.SoundFile;

public class StoryRunner {

	private static final int talkTuah = 2;
	// NOTE:
	private final PApplet parent;
	private final String mainPath;

	private final String sharedPath;
	private final String[][] mainScript;
	private int currentLine;

	private boolean hasClicked;
	private boolean hasTyped;
	private boolean hitConditional; // true if in an "if" block

	private ArrayList<Character> entities;
	private ArrayList<ExpiringObject<String[]>> foregroundCommands;

	private TreeMap<String, Object> cache; // uses paths
	private TreeMap<String, Character> entityCache; // uses filenames in mainPath/characters/ AND shared/characters/
	private TreeMap<String, Double> numCache; // uses names provided by user
	private TreeMap<String, Voice> voiceCache; // uses names provided by user

	private String messageSpeaker;
	private String message;
	private String[] submessages;
	private String messageType;
	private int messageLength;
	private PFont mainFont;
	private Voice messageVoice;
	private boolean rollText;

	private String backgroundType;
	private String backgroundData;

	public StoryRunner(PApplet parent, String mainPath, String sharedPath) {
		this.parent = parent;
		this.mainPath = mainPath;
		this.sharedPath = sharedPath;

		mainScript = parseFile(Paths.get(mainPath + "/main.str"));
		currentLine = 0;
		entities = new ArrayList<Character>();
		foregroundCommands = new ArrayList<ExpiringObject<String[]>>();

		cache = new TreeMap<String, Object>();
		entityCache = new TreeMap<String, Character>();
		numCache = new TreeMap<String, Double>();
		voiceCache = new TreeMap<String, Voice>();
		voiceCache.put("default", new Voice(this, "blank", new String[0]));

		mainFont = parent.createFont("data/dejavu-sans.book.ttf", 50);

		initCharacters(); // NOTICE HOW MAIN **OVERRIDES** SHARED
		setup();
	}

	private String[][] parseFile(Path path) {
		ArrayList<String> scriptLines;
		String[][] script = null;

		try {
			scriptLines = (ArrayList<String>) Files.readAllLines(path);
			scriptLines.replaceAll(s -> s.trim());
			scriptLines.removeIf(s -> s.equals("") || s.charAt(0) == '/');
			ArrayList<String[]> tempScript = new ArrayList<String[]>();

			for (int i = 0; i < scriptLines.size(); i++) {
				String[] line = scriptLines.get(i).trim().split("\\s+");
				String[] line2 = null;

				String accum = "";
				boolean isBuildingString = false;

				switch (line[0]) {
				case "newMessage":
					line2 = new String[1];
					line2[0] = "awaitMessage";
					break;
				case "newQuestion":
					line2 = new String[1];
					line2[0] = "awaitQuestion";
					break;
				}

				for (int ii = 1; ii < line.length; ii++) { // there's definitely a way to regex this but i cba
					if (line[ii].charAt(0) == '"' && !isBuildingString) {
						if (line[ii].charAt(line[ii].length() - 1) == '"') {
							if (line[ii].length() == 2) {
								line[ii] = " ";
							}
							else {
								line[ii] = line[ii].substring(1, line[ii].length() - 1);
							}
						}
						else {
							accum += line[ii].substring(1) + " ";
							line[ii] = "";
							isBuildingString = true;
						}
					}
					else if (line[ii].charAt(line[ii].length() - 1) == '"') {
						accum += line[ii].substring(0, line[ii].length() - 1);
						line[ii] = accum;
						accum = "";
						isBuildingString = false;
					}
					else if (isBuildingString) {
						accum += line[ii] + " ";
						line[ii] = "";
					}
				}

				tempScript.add(Arrays.stream(line).filter(s -> s != "").toArray(String[]::new));

				if (line2 != null)
					tempScript.add(line2);
			}

			script = tempScript.toArray(String[][]::new);
		}
		catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		return script;
	}

	private void initCharacters() {
		File mainCharacterFolder = new File(mainPath + "/characters");
		File sharedCharacterFolder = new File(sharedPath + "/characters");

		if (mainCharacterFolder.listFiles() != null) {
			for (final File charFile : mainCharacterFolder.listFiles()) {
				entityCache.put(charFile.getName(),
						new Character(this, parseFile(charFile.toPath()), charFile.getName()));
			}
		}
		else {
			System.err.println("WARNING: no folder named \"" + mainPath + "/characters" + "\", resuming operation");
		}

		for (final File charFile : sharedCharacterFolder.listFiles()) {
			if (!entityCache.containsKey(charFile.getName())) {
				entityCache.put(charFile.getName(),
						new Character(this, parseFile(charFile.toPath()), charFile.getName()));
			}
		}
	}

	private void setup() {
		while (!mainScript[currentLine][0].equals("setupOver")) {
			execute(mainScript[currentLine]);
			currentLine++;
		}
		currentLine++;
	}
	
	private String[] trim(String[] a, int s) {
		return Arrays.copyOfRange(a, s, a.length);
	}

	public void draw() {
		advanceLine();

		drawBackground();
		drawCharacters();
		drawMessage();
		drawForeground();
	}

	public TreeMap<String, Voice> getVoiceCache() {
		return voiceCache;
	}

	public PApplet getParent() {
		return parent;
	}

	public void mouseClicked() {
		hasClicked = true;
	}

	public void keyPressed(char key, int keyCode) {
		if (key == CODED) {
			return;
		}
		if (key == ' ') {
			mouseClicked();
			return;
		}

		numCache.put("key", (double) key);
		hasTyped = true;
	}

	private void advanceLine() {
		boolean isEnding = false;

		while (currentLine < mainScript.length && !isEnding) {
			switch (mainScript[currentLine][0]) {
			case "awaitClick":
				isEnding = !hasClicked;
				hasClicked = false;
				break;
			case "awaitKeyPress":
				isEnding = !hasTyped;
				hasTyped = false;
				break;
			case "awaitMessage":
				if (isSpeaking()) {
					if (hasClicked && messageLength != 0) {
						messageLength = message.length();
					}
					isEnding = true;
				}
				else {
					isEnding = !hasClicked;
				}
				
				if (!isEnding) {
					messageVoice.end();
				}

				hasClicked = false;
				break;
			case "awaitQuestion":
				if (!hasClicked) {
					isEnding = true;
					break;
				}
				
				float spacing = 60;
				float accum = (650 / 2) - (spacing / 2) * (submessages.length - 1);
				
				for (int i = 0; i < submessages.length; i++) {
//					parent.rect(800, accum, 500, 50);
					boolean inButton = 500 / 2 >= Math.abs(800 - parent.mouseX) &&
										50 / 2 >= Math.abs(accum - parent.mouseY);
					
					if (inButton) {
						numCache.put("choice", (double) i);
						System.out.println((double) i);
					}
					
					isEnding |= inButton;

					accum += spacing;
				}
				
				isEnding = !isEnding;
				hasClicked = false;
				break;
			case "if":
				// while condition fails OR if-else chain ends
				boolean eval = conditional(mainScript[currentLine]);
				
				while (!eval && advanceToElse()) {
					
				}
				
				break;
			case "else":
				
				break;
			case "end": // blank block, just dont execute
				break;
			default:
				execute(mainScript[currentLine]);
				System.out.println(Arrays.toString(mainScript[currentLine]));
			}

			currentLine++;
		}

		currentLine--;
		hasClicked = false;
		hasTyped = false;
	}

	/*
	 * how to structure conditionals: (tab optional)
	 * if ...
	 * 		block
	 * 		if ...
	 * 			block
	 * 		else if ...
	 * 			if ...
	 * 				block
	 * 			end
	 * 		else if
	 * 			block
	 * 		else
	 * 			block
	 * 		end
	 * end
	 */
	/*
	 * NOTES: (READ)
	 * what works:
	 * 	if
	 * 	else
	 * 	else if
	 * conditionals "chain" with each other
	 * end each conditional chain with an "end"
	 * 	yes even "else"
	 * 	1:1 of "if" and "end" lines
	 */
	private boolean advanceToElse() {
		int countif = 1;
		
		while (currentLine < mainScript.length && countif > 0) {
			currentLine++;
			
			if (mainScript[currentLine][0].equals("if"))
				countif++;
			else if (mainScript[currentLine][0].equals("else")) {
				if (countif == 1)
					return true; // ended on "else"
			}
			else if (mainScript[currentLine][0].equals("end"))
				countif--;
		}
		
		return false; // ended on "end"
	}
	
	private void advanceToEnd() {
		int countif = 1;
		
		while (currentLine < mainScript.length && countif > 0) {
			currentLine++;
			
			if (mainScript[currentLine][0].equals("if"))
				countif++;
//			else if (mainScript[currentLine][0].equals("else")) {
//				if (countif == 1)
//					break;
//			}
			else if (mainScript[currentLine][0].equals("end"))
				countif--;
		}
	}
	
	// if (lt/gt/le/ge/eq) (map key) (str/num) (map key/literal)
	private boolean conditional(String[] line) {
		double v1 = numCache.get(line[2]);
		double v2 = -1;
		
		switch (line[3]) {
		case "str":
			v2 = numCache.get(line[4]);
			break;
		case "num":
			v2 = Double.parseDouble(line[4]);
			break;
		default:
			throw new IllegalArgumentException(line[3] + " is not a valid option");
		}
		
		boolean ret = false;
		
		switch (line[1]) {
		case "lt":
			ret = v1 < v2;
			break;
		case "gt":
			ret = v1 > v2;
			break;
		case "le":
			ret = v1 <= v2;
			break;
		case "ge":
			ret = v1 >= v2;
			break;
		case "eq":
			ret = v1 == v2;
			break;
		default:
			throw new IllegalArgumentException(line[3] + " is not a valid option");
		}
		
		return ret;
	}

	// draw all characters equally spaced out
	private void drawCharacters() {
		if (entities.size() == 0)
			return;

		float spacing = parent.width / (entities.size() + 1);
		float accum = spacing;

		Iterator<Character> it = entities.iterator();
		while (it.hasNext()) {
			Character c = it.next();
			c.draw(accum, 400);
			accum += spacing;
		}
	}

	private void drawForeground() { // might make a custom iterator in the future but no real need
		Iterator<ExpiringObject<String[]>> it = foregroundCommands.iterator();
		while (it.hasNext()) {
			ExpiringObject<String[]> line = it.next();
			line.update();

			if (line.isExpired()) {
				it.remove();
				continue;
			}
			execute(line);
		}
	}
	
	private void drawMessage() {
		switch (messageType) {
		case "newQuestion":
			drawTextBox();
			drawQuestion();
			break;
		case "newMessage":
			drawTextBox();
			break;
		}
	}
	
	private void drawQuestion() {
		parent.rectMode(CENTER);
		
		if (submessages.length == 0)
			return;

		float spacing = 60;
		float accum = (650 / 2) - (spacing / 2) * (submessages.length - 1);
//		System.out.println(accum);

		for (String s : submessages) {
//			parent.stroke(0xa0e996d7);
			parent.stroke(0xFFFFFFFF);
			parent.strokeWeight(5);
			parent.fill(0xd0e996d7);
			parent.rect(800, accum, 500, 50, 20);
			
			parent.fill(0xFFFFFFFF);
			parent.textAlign(CENTER, CENTER);
			parent.textSize(35);
			parent.text(s, 800, accum - 5, 500, 50);
			
			accum += spacing;
		}
	}

	private void drawTextBox() {
		parent.rectMode(CORNER);
		messageLength += talkTuah;

		String messageSplit = message.substring(0, Math.min(messageLength, message.length()));

		// draw box
		parent.stroke(0xFFFFFFFF);
		parent.strokeWeight(5);
		parent.fill(0xd0e996d7);
		parent.rect(25, 650, 1550, 225, 20);
		parent.rect(75, 600, 250, 50, 20, 20, 0, 0);

		// reset for text + draw speaker
		parent.textFont(mainFont);
		parent.fill(0xFFFFFFFF);
		parent.textAlign(CENTER, CENTER);
		parent.textSize(35);
		parent.text(messageSpeaker, 200, 622);

		parent.textAlign(LEFT, TOP);
		parent.text(messageSplit, 40, 665, 1520, 210);
	}

	private boolean isSpeaking() {
		return messageLength < message.length();
	}

	private void execute(ExpiringObject<String[]> line) {
		String[] lineO = line.getObject();

		switch (lineO[0]) {
		case "flashbang":
			flashbang(line.getInitTime(), line.getTime());
			break;
		default:
			execute(lineO);
		}
	}

	private void execute(String[] line) {
		String[] newLine;

		switch (line[0]) {
		case "char":
			newLine = Arrays.copyOfRange(line, 1, line.length);
			executeCharacter(newLine);
			break;
		case "math":
			newLine = Arrays.copyOfRange(line, 1, line.length);
			executeMath(newLine);
			break;
		case "newMessage":
		case "newQuestion":
			messageType = line[0];
			messageSpeaker = line[1];
			message = line[2];
			messageLength = rollText ? 0 : 100000;

			messageVoice = voiceCache.getOrDefault(messageSpeaker, voiceCache.get("default"));
			
			if (line.length > 3)
				submessages = Arrays.copyOfRange(line, 3, line.length);
			else
				submessages = null; // debug - this WILL cause errors
			
			messageVoice.play();
			break;
		case "newVoice":
			voiceCache.put(line[1], new Voice(this, line[2], Arrays.copyOfRange(line, 3, line.length)));
			break;
		case "rollText":
			rollText = Boolean.parseBoolean(line[1]);
			break;
		case "setBackdrop":
			backgroundType = line[1];
			backgroundData = line[2];
			break;
		case "flashbang":
			foregroundCommands.add(getExpiry(line));
			break;
		case "exit":
			System.out.println("Done!");
			System.exit(0);
		case "load":
			loadFile(line[1], line[2]);
			// case "awaitClick": case "awaitMessage": case "awaitKeyPress": case
			// "awaitKeyPress":
			break; // TODO idk wtf i broke but fix pls
		default:
			throw new IllegalArgumentException(line[0] + " is not a valid function");
		}
	}

	private void executeCharacter(String[] line) { // note that commands applying to a character only affect the first
													// occurrence
		switch (line[0]) {
		case "add":
			if (entityCache.containsKey(line[1])) {
				entities.add(entityCache.get(line[1]));
				entities.get(entities.indexOf(new Character(line[1]))).setEmotion(line[2]);
				break;
			}
			else {
				throw new IllegalArgumentException(
						line[1] + " is not a character (try putting the init file in \"/characters\"?)");
			}
		case "remove":
			int i = entities.indexOf(new Character(line[1]));
			entities.get(i).getEffects().clear();
			entities.remove(i);
			break;
		case "removeAll":
			while (entities.size() != 0) {
				entities.get(entities.size() - 1).getEffects().clear();
				entities.remove(entities.size() - 1);
			}
			break;
		case "emote":
			entities.get(entities.indexOf(new Character(line[1]))).setEmotion(line[2]);
			break;
		case "effect":
			entities.get(entities.indexOf(new Character(line[1]))).addEffect(Arrays.copyOfRange(line, 2, line.length));
			break;
		default:
			throw new IllegalArgumentException(line[0] + " is not a valid character function");
		}
	}

	private void executeMath(String[] line) {
		switch (line[0]) {
		case "set":
			numCache.put(line[1], Double.parseDouble(line[2]));
			break;
		case "inc":
			numCache.put(line[1], numCache.get(line[1]) + 1);
			break;
		case "addEntry":
			numCache.put(line[1], numCache.get(line[1]) + numCache.get(line[2]));
			break;
		case "addNum":
			numCache.put(line[1], numCache.get(line[1]) + Double.parseDouble(line[2]));
			break;
		case "comp":
			numCache.put(line[1], (double) Double.compare(numCache.get(line[2]), numCache.get(line[3])));
			break;
		}

//		System.out.println(numCache.get(line[1]));
	}

	public void setNum(String name, double data) {
		numCache.put(name, data);
	}

	public void incNum(String name, double data) {
		numCache.put(name, data + numCache.get(name));
	}

	public double getNum(String name) {
		return numCache.get(name);
	}

	private ExpiringObject<String[]> getExpiry(String[] line) {
		switch (line[0]) {
		case "flashbang":
			if (Integer.parseInt(line[1]) > 0) {
				return new ExpiringObject<String[]>(line, Integer.parseInt(line[1]));
			}
		default:
			throw new IllegalArgumentException(line[0] + ": bad args for this, debug time");
		}
	}

	public Object getFile(String cast, String path) {
		if (!cache.containsKey(path)) {
			loadFile(cast, path);
			System.err.println("Cache miss on " + path);
		}

		return cache.get(path);
	}

	public void loadFile(String cast, String path) {
		Object obj = null;

		switch (cast) {
		case "image":
			if (new File(mainPath + path).isFile()) {
				obj = parent.loadImage(mainPath.substring(2) + path);
			}
			else if (new File(sharedPath + path).isFile()) {
				obj = parent.loadImage(sharedPath.substring(2) + path);
			}

			break;
		case "font":
			if (new File(mainPath + path).isFile()) {
				obj = parent.createFont(mainPath.substring(2) + path, 50);
			}
			else if (new File(sharedPath + path).isFile()) {
				obj = parent.createFont(sharedPath.substring(2) + path, 50);
			}

			break;
		case "sound":
			if (new File(mainPath + path).isFile()) {
				obj = new SoundFile(parent, mainPath.substring(2) + path);
			}
			else if (new File(sharedPath + path).isFile()) {
				obj = new SoundFile(parent, sharedPath.substring(2) + path);
			}

			break;
		default:
			throw new IllegalArgumentException(cast + " is not a valid file type");
		}

		if (obj != null) {
			cache.put(path, obj);
		}
		else {
			System.err.println("Invalid file found at: " + path);
		}
	}

	private void drawBackground() {
		switch (backgroundType) {
		case "hex":
			parent.background(Integer.parseInt(backgroundData, 16));
			break;
		case "image":
			parent.imageMode(CORNER);
			parent.image((PImage) getFile("image", backgroundData), 0, 0, parent.width, parent.height);
			break;
		default:
			throw new IllegalArgumentException(backgroundType + " is not a valid type for background");
		}
	}

	private void flashbang(int initTime, int time) {
		float progress = (float) (initTime - time) / initTime;

		parent.rectMode(CORNER);
		parent.fill(255, 255 * (1 - progress));
		parent.rect(0, 0, parent.width, parent.height);
	}
}
