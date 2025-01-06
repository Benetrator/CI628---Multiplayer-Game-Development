#ifndef __MY_GAME_H__
#define __MY_GAME_H__

#include <iostream>
#include <vector>
#include <string>
#include <unordered_map>
#include "SDL.h"
#include <SDL_image.h>
#include <SDL_ttf.h>
#include <iostream>

// Holds game data like player positions and rotations
struct GameData {
    int player1Y = 0;
    int player1X = 0;
    float player1Rotations = 0.0f;
    int player2Y = 0;
    int player2X = 0;
    float player2Rotations = 0.0f;

};

class Bullet {
public:
    float x = 0.0f;
    float y = 0.0f;
    SDL_Texture* texture = nullptr;

    float angle;     
    float speed;      
    bool active;
};

class Wall {
public:
    float x = 0.0f;
    float y = 0.0f;
};

class MyGame {
private:
    int player1Score;
    int player2Score;


    TTF_Font* font = nullptr;
    SDL_Color textColour = { 255, 255, 255, 255 };

    SDL_Rect player1 = { 0, 0, 20, 20 };
    SDL_Rect player2 = { 0, 0, 20, 20 };
    GameData game_data;

    int mapWidth = 26;
    int mapHeight = 19;
    int** currentMap;
    void initializeMap();



public:
    bool init();
    std::vector<std::string> messages;
    void on_receive(std::string message, std::vector<std::string>& args);
    void send(std::string message);
    void input(SDL_Event& event);
    void update();
    void render(SDL_Renderer* renderer);
    void LoadTextures(SDL_Renderer* renderer);
    void Cleanup();
    void RenderText(SDL_Renderer* renderer, const std::string& text, int x, int y);

    void setWall(int x, int y);
    void removeWall(int x, int y);
    void updateWalls(const std::vector<std::pair<int, int>>& walls);
    void printMap();
    void clearMap();
    int** getCurrentMap();
};



#endif
