package org.systemsbiology.genomebrowser.app;



public class Configurator {
	private Options options;
	private ConfStrategy strategy;


	public Configurator(Options options) {
		this.options = options;
	}

	public void setConfStrategy(ConfStrategy strategy) {
		this.strategy = strategy;
	}
	
	public Application createApplication() {
		Application app = new Application(options);
		strategy.configure(app);
		return app;
	}

	public interface ConfStrategy {
		public void configure(Application app);
	}
}
