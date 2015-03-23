package edu.pitt.ece2161.spring2015.optiplayer;

/**
 * This class will build renders needed for a specific type of video playback
 * for the ExoPlayer-based video player.
 * 
 * @author Brian Rupert
 *
 */
public interface ExoRendererBuilder {

	/**
	 * Called to generate the renderers that will be used by the player.
	 * @param player The player that renderers are being built for.
	 */
    void buildRenderers(CustomPlayer player);
}
