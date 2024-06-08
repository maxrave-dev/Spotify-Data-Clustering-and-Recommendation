import json
import pickle

import numpy as np
import pandas as pd
from flask import Flask, jsonify
from sklearn.metrics.pairwise import cosine_similarity

app = Flask(__name__)


import spotipy
from spotipy.oauth2 import SpotifyClientCredentials

auth_manager = SpotifyClientCredentials(client_id="721d6f670f074b1497e74fc59125a6f3", client_secret="efddc083fa974d39bc6369a892c07ced")
sp = spotipy.Spotify(auth_manager=auth_manager)

@app.route("/")
def intro():
    return "<p>Music Recommendation using K-means Cluster</p>"

@app.route("/recommend/<string:track_id>")
def recommend(track_id):
    track = sp.audio_features(track_id)[0]
    print(track)
    return get_list_track(recommend_util(track))


def load_model():
    import pickle
    with open("/home/maxrave/PycharmProjects/Music Recommendation/saved_model/kmean_model.sav", "rb") as f:
        model = pickle.load(f)
    return model

def recommend_util(track, num_songs=10):
    model = load_model()
    scaler = pickle.load(open('/home/maxrave/PycharmProjects/Music Recommendation/scaler.sav', 'rb'))
    music_features = ["danceability", "energy", "key", "loudness", "mode", "speechiness", "acousticness", "instrumentalness", "liveness", "valence", "tempo"]
    mfs = [track[feature] for feature in music_features]
    print(f"Music Feature",mfs)
    mfs_scaled = scaler.transform([mfs])
    print(f"Music Feature Scaled",mfs_scaled)
    cluster = model.predict(mfs_scaled)[0]
    print(f"Cluster",cluster)
    data_labeled = pd.read_csv('/home/maxrave/PycharmProjects/Music Recommendation/saved_data/k_mean_data_labels.csv')
    same_cluster_songs = data_labeled.loc[data_labeled['cluster'] == cluster]
    print(f"Same Cluster",same_cluster_songs.shape)
    music_features_ar = same_cluster_songs[['danceability', 'energy', 'key',
                                            'loudness', 'mode', 'speechiness', 'acousticness',
                                            'instrumentalness', 'liveness', 'valence', 'tempo']].values
    music_features_ar_scaled = scaler.transform(music_features_ar)
    print(f"Music_feature_ar",music_features_ar.shape)

    similarity_scores = cosine_similarity(np.array(mfs_scaled), np.array(music_features_ar_scaled))

    # Get the indices of the most similar songs
    # Get first 100 song indices
    print(similarity_scores.shape)
    similar_song_indices = similarity_scores.argsort()[0][::-1][2:(num_songs+2)]
    print(similar_song_indices)

    # Sau đó sẽ sắp xếp dựa trên popularity
    content_based_recommendations = same_cluster_songs.iloc[similar_song_indices, :].sort_values('popularity', ascending=False)

    return json.loads(content_based_recommendations.to_json(orient="records"))

def get_list_track(tracks):
    track_ids = [obj['track_id'] for obj in tracks if 'track_id' in obj]
    return sp.tracks(track_ids)


if __name__ == "__main__":
    app.run()