#include "MyGame.h"

SDL_Texture* player1Texture;
SDL_Texture* player2Texture;
SDL_Texture* wallTexture;



int bulletLength;



Bullet bullets[100];


void MyGame::on_receive(std::string cmd, std::vector<std::string>& args) {
    if (cmd == "GAME_DATA") {
        if (args.size() >= 6) {

            game_data.player1X = std::stoi(args.at(0));
            game_data.player1Y = std::stoi(args.at(1));
            game_data.player1Rotations = std::stoi(args.at(2));

            game_data.player2X = std::stoi(args.at(3));
            game_data.player2Y = std::stoi(args.at(4));
            game_data.player2Rotations = std::stoi(args.at(5));


            if (args.size() > 6) {
                bulletLength = std::stoi(args.at(6));  
                if (bulletLength > 0) {
                    
                    if (args.size() < 7 + bulletLength * 2) {
                        std::cerr << "Error: Insufficient data for " << bulletLength << " bullets." << std::endl;
                    }
                    else {
                        // Populate the bullets array
                        for (size_t i = 0; i < bulletLength; ++i) {
                            int bulletIndex = 7 + i * 2;
                            if (bulletIndex + 1 < args.size()) {  
                                bullets[i].x = std::stoi(args.at(bulletIndex));
                                bullets[i].y = std::stoi(args.at(bulletIndex + 1));
                            }
                            else {
                                std::cerr << "Error: Insufficient data for bullet " << i << std::endl;
                            }
                        }
                    }
                }
                else {
                    std::cerr << "Error: Invalid bullet count: " << bulletLength << std::endl;
                }
            }
        }
        else {
            std::cerr << "Error: Insufficient arguments for GAME_DATA." << std::endl;
        }
    }
    else if (cmd == "WALLS_DATA") {
        if (args.size() >= 2) {  
            clearMap();
            initializeMap();  


            for (size_t i = 0; i < args.size(); i += 2) {

                if (i + 1 < args.size()) {
                    int x = std::stoi(args.at(i));     
                    int y = std::stoi(args.at(i + 1)); 


                    setWall(x, y);
                }
                else {
                    std::cerr << "Error: Invalid wall data. Missing y coordinate for x = "
                        << args.at(i) << std::endl;
                }
            }
        }
        else {
            std::cerr << "Error: Insufficient data for WALLS_DATA." << std::endl;
        }
    }
    else if (cmd == "SCORE_DATA")
    {
        player1Score = std::stoi(args.at(0));
        player2Score = std::stoi(args.at(1));
    }
}

void MyGame::send(std::string message) {
    messages.push_back(message);
}

void MyGame::input(SDL_Event& event) {
    static std::unordered_map<SDL_Keycode, std::string> keyMap = {
        { SDLK_w, "W" }, { SDLK_a, "A" }, { SDLK_s, "S" }, { SDLK_d, "D" },
        { SDLK_SPACE, "SPACE" }
    };

    if (keyMap.find(event.key.keysym.sym) != keyMap.end()) {
        std::string keyState = keyMap[event.key.keysym.sym] + (event.type == SDL_KEYDOWN ? "_DOWN" : "_UP");
        send(keyState);
    }
}

void MyGame::update() {
    player1.x = game_data.player1Y;
    player1.y = game_data.player1X;

    player2.y = game_data.player2Y;
    player2.x = game_data.player2X;
}

void MyGame::render(SDL_Renderer* renderer) {
    if (!player1Texture || !player2Texture || !wallTexture) {
        std::cerr << "Player or wall textures not loaded!" << std::endl;
        return;
    }

    int tileSize = 32; 


    if (currentMap != nullptr) {
        for (int row = 0; row < mapHeight; row++) {
            for (int col = 0; col < mapWidth; col++) {
                if (currentMap[row][col] == 1) {  
                    SDL_Rect wallRect = { col * tileSize, row * tileSize, tileSize, tileSize };

                    SDL_RenderCopy(renderer, wallTexture, NULL, &wallRect);
                }
            }
        }
    }

    // Render player 1
    SDL_Rect player1Rect = { game_data.player1X, game_data.player1Y, 32, 32 };
    SDL_RenderCopyEx(renderer, player1Texture, NULL, &player1Rect, game_data.player1Rotations + 90, NULL, SDL_FLIP_NONE);

    // Render player 2
    SDL_Rect player2Rect = { game_data.player2X, game_data.player2Y, 32, 32 };
    SDL_RenderCopyEx(renderer, player2Texture, NULL, &player2Rect, game_data.player2Rotations + 90, NULL, SDL_FLIP_NONE);

    // Render bullets
    for (int i = 0; i < bulletLength; ++i) {
        if (bullets[i].texture) {
            SDL_Rect bulletRect = { bullets[i].x, bullets[i].y, 16, 16 };
            SDL_RenderCopy(renderer, bullets[i].texture, NULL, &bulletRect);
        }
    }

    std::string player1ScoreText = std::to_string(player1Score);
    std::string player2ScoreText = std::to_string(player2Score);

    RenderText(renderer, player1ScoreText, 150, 100);
    RenderText(renderer, player2ScoreText, 650, 100);
    


}

void MyGame::Cleanup() {
    
    if (player1Texture) {
        SDL_DestroyTexture(player1Texture);
        player1Texture = nullptr;
    }

    if (player2Texture) {
        SDL_DestroyTexture(player2Texture);
        player2Texture = nullptr;
    }

    // Free wall texture
    if (wallTexture) {
        SDL_DestroyTexture(wallTexture);
        wallTexture = nullptr;
    }

    for (int i = 0; i < bulletLength; ++i) {
        if (bullets[i].texture) {
            SDL_DestroyTexture(bullets[i].texture);
            bullets[i].texture = nullptr;
        }
    }

    if (font != nullptr) {
        TTF_CloseFont(font);
        font = nullptr;
    }

    TTF_Quit();
    SDL_Quit();
}

void MyGame::LoadTextures(SDL_Renderer* renderer) {
    // Load the texture for player 1
    player1Texture = IMG_LoadTexture(renderer, "Z:/Uni work/CI628 - Multiplayer C++/CI628 Client Master/CI628/src/assets/images/player1_image.png");
    if (!player1Texture) {
        std::cerr << "Failed to load player1 texture: " << SDL_GetError() << std::endl;
    }

    // Load the texture for player 2
    player2Texture = IMG_LoadTexture(renderer, "Z:/Uni work/CI628 - Multiplayer C++/CI628 Client Master/CI628/src/assets/images/player2_image.png");
    if (!player2Texture) {
        std::cerr << "Failed to load player2 texture: " << SDL_GetError() << std::endl;
    }

    // Load the bullet texture (once for all bullets)
    SDL_Texture* bulletTexture = IMG_LoadTexture(renderer, "Z:/Uni work/CI628 - Multiplayer C++/CI628 Client Master/CI628/src/assets/images/bullet.png");
    if (!bulletTexture) {
        std::cerr << "Failed to load bullet texture: " << SDL_GetError() << std::endl;
    }
    wallTexture = IMG_LoadTexture(renderer, "Z:/Uni work/CI628 - Multiplayer C++/CI628 Client Master/CI628/src/assets/images/wall.png");
    if (!wallTexture) {
        std::cerr << "Failed to load buwallllet texture: " << SDL_GetError() << std::endl;
    }

    // Assign bullet texture to all bullets
    for (int i = 0; i < 100; ++i) {  // Assuming 100 is the max number of bullets
        bullets[i].texture = bulletTexture;
    }
}

void MyGame::initializeMap() {
    if (currentMap == nullptr) {
        currentMap = new int* [mapHeight];
        for (int i = 0; i < mapHeight; ++i) {
            currentMap[i] = new int[mapWidth] {0};
        }
    }
}

void MyGame::setWall(int x, int y) {
    if (x >= 0 && x < mapWidth && y >= 0 && y < mapHeight) {
        currentMap[y][x] = 1;
    }
}

void MyGame::removeWall(int x, int y) {
    if (x >= 0 && x < mapWidth && y >= 0 && y < mapHeight) {
        currentMap[y][x] = 0; 
    }
}

void MyGame::updateWalls(const std::vector<std::pair<int, int>>& walls) {
    initializeMap();

    for (const auto& wall : walls) {
        setWall(wall.first, wall.second);
    }
}

void MyGame::printMap() {
    for (int row = 0; row < mapHeight; row++) {
        for (int col = 0; col < mapWidth; col++) {
            std::cout << currentMap[row][col] << " "; 
        }
        std::cout << std::endl;
    }
}

int** MyGame::getCurrentMap() {
    if (currentMap == nullptr) {
        std::cerr << "Error: currentMap is null!" << std::endl;
        initializeMap();  // Ensure it's initialized
    }
    return currentMap;
}

void MyGame::clearMap() {
    // Ensure the currentMap is properly initialized
    if (currentMap == nullptr) {
        // Allocate memory for the map if it hasn't been initialized
        currentMap = new int* [mapHeight];
        for (int i = 0; i < mapHeight; ++i) {
            currentMap[i] = new int[mapWidth];
        }
    }

    // Clear the map (reset all tiles to 0)
    for (int row = 0; row < mapHeight; row++) {
        for (int col = 0; col < mapWidth; col++) {
            currentMap[row][col] = 0;  // Set tile to empty (0)
        }
    }
}

void MyGame::RenderText(SDL_Renderer* renderer, const std::string& text, int x, int y)
{

    if (!font) {
        std::cerr << "Font is not loaded!" << std::endl;
        return;
    }

    SDL_Surface* textSurface = TTF_RenderText_Solid(font, text.c_str(), textColour);
    if (textSurface == nullptr) {
        std::cerr << "Unable to create text surface! TTF_Error: " << TTF_GetError() << std::endl;
        return;
    }

    SDL_Texture* textTexture = SDL_CreateTextureFromSurface(renderer, textSurface);
    if (textTexture == nullptr) {
        std::cerr << "Unable to create text texture! SDL_Error: " << SDL_GetError() << std::endl;
        SDL_FreeSurface(textSurface);  
        return;
    }

    SDL_Rect renderQuad = { x, y, textSurface->w, textSurface->h };

    SDL_RenderCopy(renderer, textTexture, NULL, &renderQuad);
    SDL_FreeSurface(textSurface);
    SDL_DestroyTexture(textTexture);
}

bool MyGame::init()
{
    font = TTF_OpenFont("Z:/Uni work/CI628 - Multiplayer C++/CI628 Client Master/CI628/src/assets/fonts/Domine-VariableFont_wght.ttf", 64);  
    if (font == nullptr) {
        std::cerr << "Failed to load font! TTF_Error: " << TTF_GetError() << std::endl;
        return false;
    }
}








