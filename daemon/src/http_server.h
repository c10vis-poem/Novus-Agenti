#pragma once
#include <string>
#include <functional>

struct HttpRequest {
    std::string method;
    std::string path;
    std::string body;
};

// Callback: (token, index, done) → false to abort
using StreamCallback = std::function<bool(const std::string&, int, bool)>;
using GenerateHandler = std::function<void(const std::string& body, StreamCallback)>;
using HealthHandler = std::function<bool()>;

class HttpServer {
public:
    HttpServer(int port, GenerateHandler gen_handler, HealthHandler health_handler);
    ~HttpServer();

    // Blocks forever, serving requests
    void run();
    void stop();

private:
    struct Impl;
    std::unique_ptr<Impl> impl_;
};
