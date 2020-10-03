CREATE TABLE albums (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(255) NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE TABLE songs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    album_id UUID NOT NULL REFERENCES albums,
    tracknum INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    mime_type VARCHAR(255) NOT NULL,
    song_url VARCHAR(255) NOT NULL,
    waveform_1_url VARCHAR(255) NOT NULL,
    waveform_2_url VARCHAR(255) NOT NULL,
    visible BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE TABLE media_links (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    album_id UUID NOT NULL REFERENCES albums,
    name VARCHAR(255) NOT NULL,
    icon VARCHAR(255),
    url VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

--Seed Data
INSERT INTO albums
    (title, image_url, updated_at)
VALUES
    ('Empty Shell', 'https://left-over.s3.amazonaws.com/images/empty-shell.jpg', now());

INSERT INTO songs
    (album_id, tracknum, title, mime_type, song_url, waveform_1_url, waveform_2_url, updated_at)
VALUES
    ((SELECT id FROM albums), 1, 'Can You Tell Me', 'audio/mpeg', 'https://left-over.s3.amazonaws.com/music/can-you-tell-me.mp3', 'https://left-over.s3.amazonaws.com/music/can-you-tell-me.waveform.grey.png', 'https://left-over.s3.amazonaws.com/music/can-you-tell-me.waveform.white.png', now()),
    ((SELECT id FROM albums), 2, 'Hey Everybody', 'audio/mpeg', 'https://left-over.s3.amazonaws.com/music/hey-everybody.mp3', 'https://left-over.s3.amazonaws.com/music/hey-everybody.waveform.grey.png', 'https://left-over.s3.amazonaws.com/music/hey-everybody.waveform.white.png', now()),
    ((SELECT id FROM albums), 3, 'Home', 'audio/mpeg', 'https://left-over.s3.amazonaws.com/music/home.mp3', 'https://left-over.s3.amazonaws.com/music/home.waveform.grey.png', 'https://left-over.s3.amazonaws.com/music/home.waveform.white.png', now()),
    ((SELECT id FROM albums), 4, 'For a Lifetime', 'audio/mpeg', 'https://left-over.s3.amazonaws.com/music/for-a-lifetime.mp3', 'https://left-over.s3.amazonaws.com/music/for-a-lifetime.waveform.grey.png', 'https://left-over.s3.amazonaws.com/music/for-a-lifetime.waveform.white.png', now()),
    ((SELECT id FROM albums), 5, 'Or', 'audio/mpeg', 'https://left-over.s3.amazonaws.com/music/or.mp3', 'https://left-over.s3.amazonaws.com/music/or.waveform.grey.png', 'https://left-over.s3.amazonaws.com/music/or.waveform.white.png', now()),
    ((SELECT id FROM albums), 6, 'Little Left', 'audio/mpeg', 'https://left-over.s3.amazonaws.com/music/little-left.mp3', 'https://left-over.s3.amazonaws.com/music/little-left.waveform.grey.png', 'https://left-over.s3.amazonaws.com/music/little-left.waveform.white.png', now()),
    ((SELECT id FROM albums), 7, 'I Moved On', 'audio/mpeg', 'https://left-over.s3.amazonaws.com/music/i-moved-on.mp3', 'https://left-over.s3.amazonaws.com/music/i-moved-on.waveform.grey.png', 'https://left-over.s3.amazonaws.com/music/i-moved-on.waveform.white.png', now()),
    ((SELECT id FROM albums), 8, 'Empty Shell', 'audio/mpeg', 'https://left-over.s3.amazonaws.com/music/empty-shell.mp3', 'https://left-over.s3.amazonaws.com/music/empty-shell.waveform.grey.png', 'https://left-over.s3.amazonaws.com/music/empty-shell.waveform.white.png', now()),
    ((SELECT id FROM albums), 9, 'This Time Lucky', 'audio/mpeg', 'https://left-over.s3.amazonaws.com/music/this-time-lucky.mp3', 'https://left-over.s3.amazonaws.com/music/this-time-lucky.waveform.grey.png', 'https://left-over.s3.amazonaws.com/music/this-time-lucky.waveform.white.png', now()),
    ((SELECT id FROM albums), 10, 'Learning to Breathe', 'audio/mpeg', 'https://left-over.s3.amazonaws.com/music/learning-to-breathe.mp3', 'https://left-over.s3.amazonaws.com/music/learning-to-breathe.waveform.grey.png', 'https://left-over.s3.amazonaws.com/music/learning-to-breathe.waveform.white.png', now()),
    ((SELECT id FROM albums), 11, 'Shame on You', 'audio/mpeg', 'https://left-over.s3.amazonaws.com/music/shame-on-you.mp3', 'https://left-over.s3.amazonaws.com/music/shame-on-you.waveform.grey.png', 'https://left-over.s3.amazonaws.com/music/shame-on-you.waveform.white.png', now()),
    ((SELECT id FROM albums), 12, 'The Deplorables', 'audio/mpeg', 'https://left-over.s3.amazonaws.com/music/the-deplorables.mp3', 'https://left-over.s3.amazonaws.com/music/the-deplorables.waveform.grey.png', 'https://left-over.s3.amazonaws.com/music/the-deplorables.waveform.white.png', now()),
    ((SELECT id FROM albums), 13, 'What You Do to Me', 'audio/mpeg', 'https://left-over.s3.amazonaws.com/music/what-you-do-to-me.mp3', 'https://left-over.s3.amazonaws.com/music/what-you-do-to-me.waveform.grey.png', 'https://left-over.s3.amazonaws.com/music/what-you-do-to-me.waveform.white.png', now());

INSERT INTO media_links
    (album_id, name, icon, url, updated_at)
VALUES
    ((SELECT id FROM albums), 'iTunes', 'itunes', 'https://music.apple.com/us/album/empty-shell/1384519930', now()),
    ((SELECT id FROM albums), 'YouTube', 'youtube', 'https://www.youtube.com/playlist?list=OLAK5uy_kU886POWpxZPE_3JA7TTXkRlnwHhDaJPE', now()),
    ((SELECT id FROM albums), 'Amazon', 'amazon', 'https://www.amazon.com/Empty-Shell-Left-Over/dp/B07D2Y5394', now()),
    ((SELECT id FROM albums), 'Spotify', 'spotify', 'https://open.spotify.com/album/3qLMjSSZH31JM1HgsRPXGe', now());
