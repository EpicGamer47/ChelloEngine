package storyInProcessingChris;

public class ExpiringObject<T> {
	
	private int time;
	private final int initTime;
	private int decay;
	private T object;
	
	public ExpiringObject(T object, int time, int decay) {
		this.time = time;
		this.initTime = time;
		this.decay = decay;
		this.object = object;
	}
	
	public ExpiringObject(T object, int time) {
		this.time = time;
		this.initTime = time;
		this.decay = 1;
		this.object = object;
	}
	
	public ExpiringObject(T object) {
		this.time = 1;
		this.initTime = 1;
		this.decay = 0;
		this.object = object;
	}

	public T getObject() {
		return object;
	}
	
	public int getInitTime() {
		return initTime;
	}
	
	public int getTime() {
		return time;
	}
	
	public void update() {
		time -= decay;
	}
	
	public boolean isExpired() {
		return time <= 0;
	}
	
	public boolean isExpiring() {
		return decay > 0;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o)
	        return true;
		if (o == null)
	        return false;
		if (getClass() != o.getClass())
	        return false;

		@SuppressWarnings("unchecked")
		ExpiringObject<T> e = (ExpiringObject<T>) o;
		return this.object.equals(e.getObject());
	}
}
