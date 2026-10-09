package com.nqd.nqd_lms_be.service.youtube;

import java.io.InputStream;
import java.util.List;

public interface YouTubeUploadService {

    /**
     * Check if YouTube upload service is enabled and configured.
     */
    boolean isConfigured();

    /**
     * Obtains a valid Google OAuth access token using the stored refresh token.
     */
    String getFreshAccessToken();

    /**
     * Stream a remote video file (e.g. from Lark recording URL) and upload it directly
     * to YouTube with unlisted privacy status.
     *
     * @param sourceUrl Direct download URL of the video (MP4)
     * @param title Title of the video on YouTube
     * @param description Description of the video
     * @param tags List of tags (optional)
     * @return Full YouTube video URL (e.g. https://www.youtube.com/watch?v=xxxx)
     */
    String uploadVideoFromUrl(String sourceUrl, String title, String description, List<String> tags);

    /**
     * Upload an InputStream directly to YouTube with unlisted privacy status (e.g. from Google Drive).
     *
     * @param inputStream Video binary stream
     * @param contentLength Size of video in bytes, or -1 if unknown
     * @param title Video title
     * @param description Video description
     * @param tags List of tags
     * @return Full YouTube video URL
     */
    String uploadVideoStream(InputStream inputStream, long contentLength, String title, String description, List<String> tags);

    /**
     * Get an existing playlist or create a new unlisted playlist for a classroom on YouTube.
     *
     * @param playlistTitle Title of the playlist (e.g. "Lớp 1A12 - NQD LMS")
     * @param description Description of the playlist
     * @param existingPlaylistId Existing playlist ID if known
     * @return YouTube Playlist ID (e.g. "PLxxxxxx") or empty string if failed
     */
    String getOrCreatePlaylist(String playlistTitle, String description, String existingPlaylistId);

    /**
     * Add a YouTube video to a specified playlist.
     *
     * @param playlistId The YouTube Playlist ID
     * @param videoId The YouTube Video ID
     * @return true if added successfully or already present
     */
    boolean addVideoToPlaylist(String playlistId, String videoId);
}
