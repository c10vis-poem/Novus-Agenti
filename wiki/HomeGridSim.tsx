import React, { useState } from "react";
import { HomeGridConfig } from "../types";
import { Sparkles, Sliders, Code, Eye, Save, RotateCcw, Check, Zap } from "lucide-react";

interface HomeGridSimProps {
  config: HomeGridConfig;
  setConfig: React.Dispatch<React.SetStateAction<HomeGridConfig>>;
  onApplyToKotlin: () => void;
  isApplying: boolean;
  appliedSuccess: boolean;
}

export const DEFAULT_CONFIG: HomeGridConfig = {
  cardWidthDp: 114,
  cardHeightDp: 138,
  iconSizeDp: 60,
  titleFontSizeSp: 14,
  sloganFontSizeSp: 13,
  logoText: "MØ[)u14R_11(",
  logoFontSizeSp: 44,
  logoFontFamily: "ChunkyBlocky",
  crystalScale: 1.20,
  crystalShape: "3D_Hexagonal_Gem",
  crystalWhiteSunGlow: true,
  plasmaTubeCurve: 0.5,
  plasmaTubeThickness: 3.5,
  plasmaTubeBeadsCount: 7,
  statusNodeSizeDp: 36,
  statusNodeStyle: "3D_Glossy_Sphere",
  backgroundDarkness: "#080C10",
  starsCount: 180,
  extraTelemetryClusters: 4,
  chatBarPosition: "ABOVE_STATUS_NODES",
  chatBarBorderTeal: true,
  showSubtitlesAndSlugs: true,
  promptUnderscore: true,
};

export const HomeGridSim: React.FC<HomeGridSimProps> = ({
  config,
  setConfig,
  onApplyToKotlin,
  isApplying,
  appliedSuccess,
}) => {
  const [activeTab, setActiveTab] = useState<"preview" | "controls" | "kotlin">("preview");
  const [selectedTile, setSelectedTile] = useState<string | null>(null);
  const [isHoldingChat, setIsHoldingChat] = useState(false);
  const [deviceProfile, setDeviceProfile] = useState<"razr_ultra" | "razr_outer" | "standard">("razr_ultra");

  // Draggable tile positions offset state
  const [tileOffsets, setTileOffsets] = useState<Record<string, { x: number; y: number }>>({});
  const [draggingItem, setDraggingItem] = useState<{ id: string; startX: number; startY: number; initialX: number; initialY: number } | null>(null);

  const handlePointerDown = (id: string, e: React.PointerEvent) => {
    e.stopPropagation();
    setSelectedTile(id);
    const initial = tileOffsets[id] || { x: 0, y: 0 };
    setDraggingItem({
      id,
      startX: e.clientX,
      startY: e.clientY,
      initialX: initial.x,
      initialY: initial.y,
    });
  };

  const handlePointerMove = (e: React.PointerEvent) => {
    if (!draggingItem) return;
    const deltaX = e.clientX - draggingItem.startX;
    const deltaY = e.clientY - draggingItem.startY;
    setTileOffsets((prev) => ({
      ...prev,
      [draggingItem.id]: {
        x: draggingItem.initialX + deltaX,
        y: draggingItem.initialY + deltaY,
      },
    }));
  };

  const handlePointerUp = () => {
    setDraggingItem(null);
  };

  const resetTilePositions = () => {
    setTileOffsets({});
  };

  const resetConfig = () => {
    setConfig(DEFAULT_CONFIG);
    setTileOffsets({});
  };

  return (
    <div className="max-w-7xl mx-auto p-4 md:p-6 grid grid-cols-1 lg:grid-cols-12 gap-6">
      {/* Left Column: Phone Frame & Live Compose Canvas */}
      <div className="lg:col-span-7 flex flex-col items-center">
        {/* Canvas Header Bar */}
        <div className="w-full max-w-md bg-slate-900 border border-slate-800 rounded-t-xl px-4 py-2.5 flex items-center justify-between text-xs font-mono text-slate-300">
          <div className="flex items-center gap-2">
            <span className="w-2.5 h-2.5 rounded-full bg-teal-400 animate-pulse" />
            <span className="font-semibold text-teal-300">Jetpack Compose Live Preview</span>
          </div>
          <div className="flex items-center gap-2 text-slate-400">
            <select
              value={deviceProfile}
              onChange={(e) => setDeviceProfile(e.target.value as any)}
              className="bg-slate-950 border border-slate-800 text-[11px] font-mono text-teal-300 rounded px-2 py-0.5 focus:outline-none focus:border-teal-500"
            >
              <option value="razr_ultra">Moto Razr 25 Ultra (22:9 Main Screen)</option>
              <option value="razr_outer">Moto Razr 25 Ultra (Outer Display)</option>
              <option value="standard">Standard Android Screen</option>
            </select>
            <button
              onClick={() => setActiveTab("preview")}
              className={`p-1 rounded ${activeTab === "preview" ? "text-teal-400 bg-slate-800" : "hover:text-slate-200"}`}
              title="View Canvas"
            >
              <Eye className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>

        {/* Android Phone Device Frame */}
        <div
          onPointerMove={handlePointerMove}
          onPointerUp={handlePointerUp}
          onPointerCancel={handlePointerUp}
          className={`w-full ${deviceProfile === "razr_ultra" ? "max-w-[380px] min-h-[820px]" : deviceProfile === "razr_outer" ? "max-w-[360px] min-h-[420px]" : "max-w-md min-h-[760px]"} bg-slate-950 border-2 border-slate-800 rounded-b-2xl p-2 shadow-2xl shadow-teal-950/20 relative overflow-hidden flex flex-col transition-all duration-300 touch-none select-none`}
        >
          {/* Simulated Screen Body */}
          <div
            className="w-full flex-1 rounded-xl relative flex flex-col justify-between p-3 select-none transition-colors duration-300 overflow-hidden"
            style={{ backgroundColor: config.backgroundDarkness }}
          >
            {/* Background Canvas: Stars, Telemetry & Plasma Cords */}
            <svg viewBox="0 0 400 600" preserveAspectRatio="none" className="absolute inset-0 w-full h-full pointer-events-none">
              {/* Star Field with Subtle Depth Layers */}
              {Array.from({ length: Math.min(config.starsCount, 160) }).map((_, i) => {
                const x = ((i * 37 + 13) % 100) * 4;
                const y = ((i * 59 + 7) % 100) * 6;
                const isForeground = i % 5 === 0;
                const isMidground = i % 3 === 0;
                const size = isForeground ? 1.5 : isMidground ? 1.0 : 0.6;
                const alpha = isForeground ? 0.8 : isMidground ? 0.45 : 0.2;
                const isTeal = i % 4 === 0;
                return (
                  <circle
                    key={`star-${i}`}
                    cx={x}
                    cy={y}
                    r={size}
                    fill={isTeal ? "#2DD4D9" : "#FFFFFF"}
                    opacity={alpha}
                  />
                );
              })}

              {/* Central Telemetry Rings around Hub (Center 200, 240) */}
              <circle cx="200" cy="240" r="65" stroke="#2DD4D9" strokeWidth="0.6" strokeDasharray="3 3" opacity="0.18" fill="none" />
              <circle cx="200" cy="240" r="105" stroke="#2DD4D9" strokeWidth="0.5" strokeDasharray="6 4" opacity="0.12" fill="none" />
              <circle cx="200" cy="240" r="145" stroke="#2DD4D9" strokeWidth="0.5" opacity="0.08" fill="none" />

              {/* Extra Telemetry Clusters (Spec §2) */}
              {config.extraTelemetryClusters >= 1 && (
                <g>
                  <circle cx="72" cy="110" r="24" stroke="#2DD4D9" strokeWidth="0.6" opacity="0.2" fill="none" />
                  <circle cx="72" cy="110" r="38" stroke="#2DD4D9" strokeWidth="0.5" strokeDasharray="2 2" opacity="0.12" fill="none" />
                </g>
              )}
              {config.extraTelemetryClusters >= 2 && (
                <g>
                  <circle cx="328" cy="390" r="20" stroke="#2DD4D9" strokeWidth="0.6" opacity="0.2" fill="none" />
                  <circle cx="328" cy="390" r="32" stroke="#2DD4D9" strokeWidth="0.5" opacity="0.12" fill="none" />
                </g>
              )}
              {config.extraTelemetryClusters >= 3 && (
                <g>
                  <circle cx="328" cy="110" r="18" stroke="#2DD4D9" strokeWidth="0.5" strokeDasharray="4 2" opacity="0.18" fill="none" />
                  <circle cx="72" cy="390" r="22" stroke="#2DD4D9" strokeWidth="0.5" opacity="0.15" fill="none" />
                </g>
              )}
              {config.extraTelemetryClusters >= 4 && (
                <g>
                  <circle cx="200" cy="55" r="28" stroke="#2DD4D9" strokeWidth="0.5" strokeDasharray="5 3" opacity="0.15" fill="none" />
                  <circle cx="200" cy="510" r="25" stroke="#2DD4D9" strokeWidth="0.5" strokeDasharray="3 3" opacity="0.15" fill="none" />
                </g>
              )}

              {/* Clean Curved Plasma Cords strictly from Tile Corners to Crystal Pedestal Socket Nodes */}
              {[
                { tileX: 200, tileY: 138, nodeX: 200, nodeY: 266, color: "#2DD4D9", curveX: 0, curveY: 0 },   // Monitor -> Top platform node (12:00)
                { tileX: 275, tileY: 195, nodeX: 223, nodeY: 273, color: "#4FE9A6", curveX: 20, curveY: 12 },  // Chat -> Top-Right platform node (2:00)
                { tileX: 275, tileY: 338, nodeX: 223, nodeY: 288, color: "#FF5577", curveX: 20, curveY: -12 }, // Settings -> Bottom-Right platform node (4:00)
                { tileX: 200, tileY: 362, nodeX: 200, nodeY: 294, color: "#00FF41", curveX: 0, curveY: 0 },   // Terminal -> Bottom platform node (6:00) (Straight through ROUTER label)
                { tileX: 125, tileY: 338, nodeX: 177, nodeY: 288, color: "#E8A838", curveX: -20, curveY: -12 },// Archives -> Bottom-Left platform node (8:00)
                { tileX: 125, tileY: 195, nodeX: 177, nodeY: 273, color: "#40C4FF", curveX: -20, curveY: 12 }, // Horizons -> Top-Left platform node (10:00)
              ].map((cord, idx) => {
                const controlX = (cord.tileX + cord.nodeX) / 2 + cord.curveX * config.plasmaTubeCurve * 2.2;
                const controlY = (cord.tileY + cord.nodeY) / 2 + cord.curveY * config.plasmaTubeCurve * 2.2;

                return (
                  <g key={`cord-${idx}`}>
                    {/* Corner Blend/Melting Soft Glow at Tile Attachment */}
                    <circle cx={cord.tileX} cy={cord.tileY} r="8" fill={cord.color} opacity="0.4" className="blur-[3px]" />

                    {/* Outer Neon Glow */}
                    <path
                      d={`M ${cord.tileX} ${cord.tileY} Q ${controlX} ${controlY} ${cord.nodeX} ${cord.nodeY}`}
                      stroke={cord.color}
                      strokeWidth={Math.max(3.2, config.plasmaTubeThickness * 2)}
                      strokeOpacity="0.25"
                      fill="none"
                      strokeLinecap="round"
                    />
                    {/* Main Plasma Core Tube */}
                    <path
                      d={`M ${cord.tileX} ${cord.tileY} Q ${controlX} ${controlY} ${cord.nodeX} ${cord.nodeY}`}
                      stroke={cord.color}
                      strokeWidth={Math.max(1.5, config.plasmaTubeThickness)}
                      strokeOpacity="0.9"
                      fill="none"
                      strokeLinecap="round"
                    />
                    {/* Inner Sharp Laser Core Line */}
                    <path
                      d={`M ${cord.tileX} ${cord.tileY} Q ${controlX} ${controlY} ${cord.nodeX} ${cord.nodeY}`}
                      stroke="#FFFFFF"
                      strokeWidth="0.8"
                      strokeOpacity="0.95"
                      fill="none"
                      strokeLinecap="round"
                    />
                    {/* Terminal Socket Glow Dots at Endpoints */}
                    <circle cx={cord.tileX} cy={cord.tileY} r="2.5" fill={cord.color} />
                    <circle cx={cord.tileX} cy={cord.tileY} r="1.2" fill="#FFFFFF" />
                    <circle cx={cord.nodeX} cy={cord.nodeY} r="3" fill={cord.color} />
                    <circle cx={cord.nodeX} cy={cord.nodeY} r="1.5" fill="#FFFFFF" />
                  </g>
                );
              })}
            </svg>

            {/* SECTION 1: HEADER & LOGO */}
            <div className="z-10 text-center flex flex-col items-center pt-1 pb-2 border-b border-purple-500/15">
              <div className="font-mono font-black text-teal-300 tracking-wider text-center leading-tight transition-all">
                <span style={{ fontSize: `${config.logoFontSizeSp * 0.75}px` }}>
                  MØ[)u14R
                </span>
                <span style={{ fontSize: `${config.logoFontSizeSp * 0.58}px` }} className="text-teal-400 ml-0">
                  _11(
                </span>
              </div>

              <div
                className="font-mono font-bold text-teal-400 mt-0.5 tracking-tight flex items-center justify-center gap-1"
                style={{ fontSize: `${config.sloganFontSizeSp}px` }}
              >
                <span className="font-mono text-teal-300">*Pioneer_Tech,</span>
                <span className="text-teal-200 font-extrabold">(Next-Gen Certified)</span>
              </div>

              <div className="w-full flex justify-end text-[9px] font-mono text-teal-400/50 mt-1 pr-1">
                HORIZONS // V4
              </div>
            </div>

            {/* SECTION 3, 4, 5: CLOCK WHEEL & TILES & ROUTER HUB */}
            <div className="relative flex-1 my-2 flex items-center justify-center min-h-[380px]">
              {/* Center Router Hub (3D Hexagonal Faceted Violet Crystal) */}
              <div className="absolute z-20 flex flex-col items-center justify-center cursor-pointer group">
                {/* Intense Violet Radial Sun Aura Permeating from Inside Crystal */}
                {config.crystalWhiteSunGlow && (
                  <div
                    className="absolute w-36 h-36 rounded-full bg-radial from-purple-400/40 via-violet-600/30 to-transparent blur-xl pointer-events-none opacity-90"
                    style={{ transform: `scale(${config.crystalScale})` }}
                  />
                )}

                {/* 3D Hexagonal Faceted Crystal Canvas / SVG */}
                <svg
                  width={110 * config.crystalScale}
                  height={110 * config.crystalScale}
                  viewBox="0 0 100 100"
                  className="filter drop-shadow-[0_0_16px_rgba(168,85,247,0.7)] group-hover:scale-105 transition-transform"
                >
                  <defs>
                    {/* Inner Sun Glow Radial Gradient */}
                    <radialGradient id="sunCoreGlow" cx="50%" cy="50%" r="50%">
                      <stop offset="0%" stopColor="#FFFFFF" stopOpacity="1" />
                      <stop offset="30%" stopColor="#E9D5FF" stopOpacity="0.9" />
                      <stop offset="65%" stopColor="#A855F7" stopOpacity="0.6" />
                      <stop offset="100%" stopColor="#6B21A8" stopOpacity="0" />
                    </radialGradient>
                    {/* Front Facet Gradients */}
                    <linearGradient id="crystalFrontLeft" x1="0%" y1="0%" x2="100%" y2="100%">
                      <stop offset="0%" stopColor="#9333EA" stopOpacity="0.85" />
                      <stop offset="100%" stopColor="#581C87" stopOpacity="0.9" />
                    </linearGradient>
                    <linearGradient id="crystalFrontRight" x1="0%" y1="0%" x2="100%" y2="100%">
                      <stop offset="0%" stopColor="#A855F7" stopOpacity="0.9" />
                      <stop offset="100%" stopColor="#6B21A8" stopOpacity="0.95" />
                    </linearGradient>
                    <linearGradient id="crystalCapLeft" x1="0%" y1="0%" x2="100%" y2="100%">
                      <stop offset="0%" stopColor="#C084FC" stopOpacity="0.95" />
                      <stop offset="100%" stopColor="#7E22CE" stopOpacity="0.9" />
                    </linearGradient>
                    <linearGradient id="crystalCapRight" x1="0%" y1="0%" x2="100%" y2="100%">
                      <stop offset="0%" stopColor="#E9D5FF" stopOpacity="0.95" />
                      <stop offset="100%" stopColor="#9333EA" stopOpacity="0.9" />
                    </linearGradient>
                    <linearGradient id="platformGlassGradient" x1="0%" y1="0%" x2="0%" y2="100%">
                      <stop offset="0%" stopColor="#2DD4D9" stopOpacity="0.45" />
                      <stop offset="50%" stopColor="#7E22CE" stopOpacity="0.35" />
                      <stop offset="100%" stopColor="#1E1035" stopOpacity="0.8" />
                    </linearGradient>
                    <linearGradient id="platformRimGradient" x1="0%" y1="0%" x2="100%" y2="0%">
                      <stop offset="0%" stopColor="#2DD4D9" stopOpacity="0.8" />
                      <stop offset="50%" stopColor="#A855F7" stopOpacity="0.9" />
                      <stop offset="100%" stopColor="#2DD4D9" stopOpacity="0.8" />
                    </linearGradient>
                  </defs>

                  {/* Solid 3D Glowing Platform Base Pedestal */}
                  {/* 3D Platform Base Cylinder Wall (Bottom Depth) */}
                  <path d="M 12,74 A 38,12 0 0,0 88,74 L 88,78 A 38,12 0 0,1 12,78 Z" fill="#130924" stroke="#7E22CE" strokeWidth="0.8" />
                  <ellipse cx="50" cy="78" rx="38" ry="12" fill="#0A0518" stroke="#2DD4D9" strokeWidth="0.8" opacity="0.6" />

                  {/* Top Glowing Glass Surface Disc */}
                  <ellipse cx="50" cy="74" rx="38" ry="12" fill="url(#platformGlassGradient)" stroke="url(#platformRimGradient)" strokeWidth="1.6" />
                  <ellipse cx="50" cy="74" rx="31" ry="9" fill="none" stroke="#2DD4D9" strokeWidth="1" strokeDasharray="4 2" opacity="0.85" />
                  <ellipse cx="50" cy="74" rx="22" ry="6" fill="rgba(255,255,255,0.08)" stroke="#A855F7" strokeWidth="0.8" />

                  {/* 6 Perimeter Socket Nodes on Platform Base (12:00, 2:00, 4:00, 6:00, 8:00, 10:00) */}
                  {[
                    { x: 50, y: 63 },   // Top 12:00
                    { x: 68, y: 68 },   // Top-Right 2:00
                    { x: 68, y: 80 },   // Bottom-Right 4:00
                    { x: 50, y: 85 },   // Bottom 6:00
                    { x: 32, y: 80 },   // Bottom-Left 8:00
                    { x: 32, y: 68 },   // Top-Left 10:00
                  ].map((node, i) => (
                    <g key={`plat-node-${i}`}>
                      <circle cx={node.x} cy={node.y} r="4.5" fill="#2DD4D9" opacity="0.25" />
                      <circle cx={node.x} cy={node.y} r="2.4" fill="#2DD4D9" opacity="0.95" />
                      <circle cx={node.x} cy={node.y} r="1" fill="#FFFFFF" />
                    </g>
                  ))}

                  {/* 3D Hexagonal Pointed Gem Crystal Body */}
                  {/* Back Facets Shadow */}
                  <polygon points="50,10 74,30 68,22" fill="#3B0764" opacity="0.6" />

                  {/* Front Left Facet */}
                  <polygon points="26,30 50,35 50,72 28,67" fill="url(#crystalFrontLeft)" stroke="#C084FC" strokeWidth="0.8" />
                  {/* Front Right Facet */}
                  <polygon points="50,35 74,30 72,67 50,72" fill="url(#crystalFrontRight)" stroke="#E9D5FF" strokeWidth="0.8" />
                  {/* Right Side Facet */}
                  <polygon points="74,30 80,24 78,60 72,67" fill="#4C1D95" stroke="#A855F7" strokeWidth="0.7" opacity="0.9" />

                  {/* Top Cap Left Facet */}
                  <polygon points="50,10 26,30 50,35" fill="url(#crystalCapLeft)" stroke="#E9D5FF" strokeWidth="1" />
                  {/* Top Cap Right Facet */}
                  <polygon points="50,10 50,35 74,30" fill="url(#crystalCapRight)" stroke="#FFFFFF" strokeWidth="1" />

                  {/* Intense Center White Sun / Core Light Inside Crystal */}
                  <circle cx="50" cy="50" r="22" fill="url(#sunCoreGlow)" opacity="0.95" />
                  <circle cx="50" cy="50" r="4.5" fill="#FFFFFF" />

                  {/* Sharp Specular Highlight Streak */}
                  <line x1="48" y1="12" x2="30" y2="28" stroke="#FFFFFF" strokeWidth="1.5" strokeLinecap="round" opacity="0.9" />
                </svg>

                {/* Hub Labels */}
                <div className="text-center font-mono mt-0.5 z-30 bg-[#0A0518]/90 backdrop-blur-md px-2.5 py-0.5 rounded-lg border border-purple-500/30 shadow-[0_0_12px_rgba(168,85,247,0.35)]">
                  <div className="text-[9px] text-violet-300 font-semibold">// CORE_HUB</div>
                  <div className="text-xs font-black text-white tracking-widest drop-shadow-[0_0_8px_rgba(255,255,255,0.9)]">
                    ROUTER
                  </div>
                  <div className="text-[8px] text-violet-300/80">$_Statio</div>
                </div>
              </div>

              {/* 6 Clock Wheel Tile Cards */}
              {[
                {
                  id: "monitor",
                  name: "MONITOR",
                  slug: "/cognito",
                  sub: "library",
                  cmd: "$_browser",
                  color: "#2DD4D9",
                  bg: "rgba(10, 14, 17, 0.95)",
                  pos: "top-7 left-1/2 -translate-x-1/2",
                  badge: "PC",
                  iconType: "monitor",
                },
                {
                  id: "chat",
                  name: "CHAT",
                  slug: "/interface",
                  sub: "tools",
                  cmd: "$_model",
                  color: "#4FE9A6",
                  bg: "rgba(10, 14, 17, 0.95)",
                  pos: "top-16 right-3",
                  iconType: "chat",
                },
                {
                  id: "settings",
                  name: "SETTINGS",
                  slug: "/config",
                  sub: "vault",
                  cmd: "$_utils",
                  color: "#FF5577",
                  bg: "rgba(10, 14, 17, 0.95)",
                  pos: "bottom-20 right-3",
                  iconType: "settings",
                },
                {
                  id: "terminal",
                  name: "TERMINAL",
                  slug: "/shell",
                  sub: "commands",
                  cmd: "$_bash",
                  color: "#00FF41",
                  bg: "#060A07",
                  pos: "bottom-4 left-1/2 -translate-x-1/2",
                  iconType: "terminal",
                },
                {
                  id: "archives",
                  name: "ARCHIVES",
                  slug: "/logs",
                  sub: "artifacts",
                  cmd: "$_files",
                  color: "#E8A838",
                  bg: "rgba(10, 14, 17, 0.95)",
                  pos: "bottom-20 left-3",
                  iconType: "archives",
                },
                {
                  id: "horizons",
                  name: "HORIZONS",
                  slug: "/about",
                  sub: "credits",
                  cmd: "$.home",
                  color: "#40C4FF",
                  bg: "rgba(10, 14, 17, 0.95)",
                  pos: "top-16 left-3",
                  iconType: "horizons",
                },
              ].map((tile) => {
                const isSelected = selectedTile === tile.id;
                return (
                  <div
                    key={tile.id}
                    onClick={() => setSelectedTile(tile.id)}
                    className={`absolute ${tile.pos} z-30 transition-all cursor-pointer group`}
                    style={{
                      width: `${config.cardWidthDp * 0.82}px`,
                      height: `${config.cardHeightDp * 0.82}px`,
                    }}
                  >
                    {/* Protruding Icon Overlay (Floating cleanly with backlit radial glow, NO box/border around icon) */}
                    <div className="absolute -top-5 left-1/2 -translate-x-1/2 z-40 flex flex-col items-center">
                      {/* Backlit Radial Aura Glow */}
                      <div
                        className="absolute w-12 h-12 rounded-full blur-lg opacity-40 group-hover:opacity-70 transition-opacity pointer-events-none -z-10"
                        style={{ backgroundColor: tile.color }}
                      />
                      {/* Icon Graphic Floating Without Bounding Box */}
                      <div
                        className="relative z-10 flex items-center justify-center p-0 drop-shadow-[0_2px_8px_rgba(0,0,0,0.9)]"
                        style={{
                          width: `${config.iconSizeDp * 0.9}px`,
                          height: `${config.iconSizeDp * 0.9}px`,
                        }}
                      >
                        {/* Custom Canvas/SVG Icon per Tile */}
                        {tile.iconType === "horizons" && (
                          <svg viewBox="0 0 36 36" className="w-full h-full">
                            {/* Violet Arch Dome */}
                            <path d="M 5,23 A 13,13 0 0,1 31,23" fill="none" stroke="#BB88FF" strokeWidth="2.2" strokeLinecap="round" />
                            {/* Amber Sun Rays pointing upward */}
                            <line x1="18" y1="3" x2="18" y2="8.5" stroke="#F5C518" strokeWidth="2" strokeLinecap="round" />
                            <line x1="8.5" y1="7.5" x2="12.5" y2="11.5" stroke="#F5C518" strokeWidth="2" strokeLinecap="round" />
                            <line x1="27.5" y1="7.5" x2="23.5" y2="11.5" stroke="#F5C518" strokeWidth="2" strokeLinecap="round" />
                            {/* Amber Sun Core / Iris Arc */}
                            <path d="M 12,23 A 6,6 0 0,1 24,23" fill="none" stroke="#F5C518" strokeWidth="2" />
                            <circle cx="18" cy="19" r="2.5" fill="#F5C518" />
                            {/* Blue Horizon Line */}
                            <line x1="3" y1="23" x2="33" y2="23" stroke="#40C4FF" strokeWidth="2.2" strokeLinecap="round" />
                          </svg>
                        )}
                        {tile.iconType === "monitor" && (
                          <svg viewBox="0 0 36 36" className="w-full h-full">
                            {/* Display Screen */}
                            <rect x="4" y="6" width="28" height="18" rx="3" fill="#0A0E11" stroke="#2DD4D9" strokeWidth="2" />
                            <line x1="8" y1="12" x2="22" y2="12" stroke="#2DD4D9" strokeWidth="1.2" opacity="0.8" />
                            <line x1="8" y1="17" x2="18" y2="17" stroke="#2DD4D9" strokeWidth="1.2" opacity="0.6" />
                            {/* Stand */}
                            <path d="M 12,28 L 18,24 L 24,28" fill="none" stroke="#2DD4D9" strokeWidth="2" strokeLinecap="round" />
                            {/* PC Badge Upper Right Corner */}
                            <rect x="23" y="4" width="10" height="8" rx="2" fill="#2DD4D9" />
                            <text x="28" y="10" fontSize="5.5" fontWeight="900" fill="#0A0E11" textAnchor="middle" fontFamily="monospace">PC</text>
                          </svg>
                        )}
                        {tile.iconType === "chat" && (
                          <svg viewBox="0 0 36 36" className="w-full h-full">
                            {/* Speech Bubble */}
                            <rect x="4" y="5" width="28" height="20" rx="5" fill="none" stroke="#4FE9A6" strokeWidth="2.5" />
                            <path d="M 10,25 L 8,31 L 16,25 Z" fill="#4FE9A6" stroke="#4FE9A6" strokeWidth="1" />
                            {/* 2 Inner Horizontal Lines */}
                            <line x1="10" y1="11" x2="24" y2="11" stroke="#4FE9A6" strokeWidth="2" strokeLinecap="round" />
                            <line x1="10" y1="16" x2="19" y2="16" stroke="#4FE9A6" strokeWidth="2" strokeLinecap="round" />
                          </svg>
                        )}
                        {tile.iconType === "terminal" && (
                          <svg viewBox="0 0 36 36" className="w-full h-full">
                            {/* Matrix Green Window */}
                            <rect x="4" y="6" width="28" height="20" rx="3" fill="#060A07" stroke="#00FF41" strokeWidth="2" />
                            {/* Header Dots */}
                            <circle cx="8" cy="10" r="1.2" fill="#00FF41" />
                            <circle cx="12" cy="10" r="1.2" fill="#00FF41" />
                            <circle cx="16" cy="10" r="1.2" fill="#00FF41" />
                            <line x1="4" y1="14" x2="32" y2="14" stroke="#00FF41" strokeWidth="0.8" opacity="0.4" />
                            {/* Prompt Cursor */}
                            <polyline points="8,18 13,21 8,24" fill="none" stroke="#00FF41" strokeWidth="1.8" strokeLinecap="round" />
                            <line x1="15" y1="24" x2="22" y2="24" stroke="#00FF41" strokeWidth="1.8" strokeLinecap="round" />
                          </svg>
                        )}
                        {/* Archives / Artifacts Icon */}
                        {tile.iconType === "archives" && (
                          <svg viewBox="0 0 36 36" className="w-full h-full">
                            {/* Main Document (Back / Left) */}
                            <rect x="5" y="2" width="18" height="24" rx="3" fill="#0A0E11" stroke="#E8A838" strokeWidth="2.2" strokeLinejoin="round" />
                            <line x1="9" y1="7" x2="18" y2="7" stroke="#E8A838" strokeWidth="1.8" strokeLinecap="round" />
                            <line x1="9" y1="12" x2="18" y2="12" stroke="#E8A838" strokeWidth="1.8" strokeLinecap="round" />
                            <line x1="9" y1="17" x2="14" y2="17" stroke="#E8A838" strokeWidth="1.8" strokeLinecap="round" />
                            {/* Overlapping Badge Document (Front / Right) */}
                            <rect x="15" y="12" width="16" height="21" rx="3" fill="#0A0E11" stroke="#E8A838" strokeWidth="2.2" strokeLinejoin="round" />
                            <text x="23" y="26.5" fontSize="13" fontWeight="900" fill="#E8A838" textAnchor="middle" fontFamily="sans-serif">A</text>
                          </svg>
                        )}
                        {tile.iconType === "settings" && (
                          <svg viewBox="0 0 36 36" className="w-full h-full">
                            {/* 8 Outer Notches/Ticks */}
                            {[0, 45, 90, 135, 180, 225, 270, 315].map((deg, i) => {
                              const rad = (deg * Math.PI) / 180;
                              const x1 = 18 + 10.5 * Math.cos(rad);
                              const y1 = 18 + 10.5 * Math.sin(rad);
                              const x2 = 18 + 13.5 * Math.cos(rad);
                              const y2 = 18 + 13.5 * Math.sin(rad);
                              return (
                                <line
                                  key={`setting-tick-${i}`}
                                  x1={x1}
                                  y1={y1}
                                  x2={x2}
                                  y2={y2}
                                  stroke="#FF5577"
                                  strokeWidth="2.2"
                                  strokeLinecap="round"
                                />
                              );
                            })}
                            {/* Outer Dashed Ring */}
                            <circle cx="18" cy="18" r="10.5" fill="none" stroke="#FF5577" strokeWidth="1" strokeDasharray="3 2" opacity="0.8" />
                            {/* Inner Crimson Circle */}
                            <circle cx="18" cy="18" r="7.5" fill="#FF5577" />
                            {/* Crisp Yellow Lightning Bolt */}
                            <polygon points="18.5,12 14.2,18 17.2,18 15.8,24 21.8,17 18.8,17" fill="#F5C518" />
                          </svg>
                        )}
                      </div>
                    </div>

                    {/* Tile Card Body */}
                    <div
                      className={`w-full h-full pt-10 pb-2 px-2 rounded-xl flex flex-col justify-between border transition-all ${
                        isSelected ? "ring-2 ring-teal-400 border-teal-300" : ""
                      }`}
                      style={{
                        backgroundColor: tile.bg,
                        borderColor: `${tile.color}44`,
                        boxShadow: `0 4px 20px rgba(0,0,0,0.8), inset 0 1px 0 ${tile.color}33`,
                      }}
                    >
                      {/* Title */}
                      <div
                        className="font-mono font-bold text-center tracking-wider mt-1"
                        style={{ color: tile.color, fontSize: `${config.titleFontSizeSp * 0.75}px` }}
                      >
                        {tile.name}
                      </div>

                      {/* Slug + Descriptor Subtitle */}
                      {config.showSubtitlesAndSlugs && (
                        <div
                          className="font-mono text-[8px] text-center text-slate-400 font-medium leading-tight my-0.5 tracking-tight"
                        >
                          {tile.slug} · {tile.sub}
                        </div>
                      )}

                      <div className="w-full h-[1px] my-1" style={{ backgroundColor: `${tile.color}22` }} />

                      {/* Bordered Prompt Box ($_command ⚙) */}
                      <div
                        className="flex items-center justify-between font-mono text-[8px] px-1.5 py-0.5 rounded border"
                        style={{
                          backgroundColor: `${tile.color}10`,
                          borderColor: `${tile.color}33`,
                          color: tile.color,
                        }}
                      >
                        <span className="font-semibold">{tile.cmd}</span>
                        <span className="opacity-60">⚙</span>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>

            {/* SECTION 7: CHAT INPUT BAR (ABOVE status nodes per spec §7) */}
            <div className="z-20 w-full px-2 mb-2">
              <div
                onMouseDown={() => setIsHoldingChat(true)}
                onMouseUp={() => setIsHoldingChat(false)}
                className={`w-full rounded-full bg-slate-950 border px-3 py-2 flex items-center justify-between font-mono text-xs cursor-pointer transition-all shadow-lg ${
                  config.chatBarBorderTeal ? "border-teal-400/40 hover:border-teal-400" : "border-slate-700"
                } ${isHoldingChat ? "scale-98 ring-2 ring-teal-400/50 bg-slate-900" : ""}`}
              >
                <div className="flex items-center gap-2 text-teal-400">
                  <span className="font-bold text-sm">⊕</span>
                  <span className="text-teal-300/80 text-[11px]">
                    {isHoldingChat ? "holding -> expanding mini inference UI..." : "tap_or_hold  ask //"}
                  </span>
                </div>
                <span className="text-teal-400 font-bold text-sm">↑</span>
              </div>

              {/* Hold-to-Expand Mini Inference UI Preview Drawer */}
              {isHoldingChat && (
                <div className="mt-2 p-3 bg-slate-900/95 border border-teal-500/40 rounded-xl font-mono text-xs text-teal-300 animate-in fade-in duration-200">
                  <div className="flex justify-between items-center text-[10px] text-teal-400/70 border-b border-teal-500/20 pb-1 mb-2">
                    <span>// MINI_INFERENCE_EXPANSION</span>
                    <span>Qwen-2.5 7B Ready</span>
                  </div>
                  <div className="text-slate-300 text-[11px]">
                    &gt; Executing live prompt stream over local GenieX backend...
                  </div>
                </div>
              )}
            </div>

            {/* SECTION 8: SYSTEM STATUS NODES (BELOW chat bar per spec §8) */}
            <div className="z-20 w-full px-2 mb-2 pb-2">
              <div className="w-full bg-slate-950/90 border border-slate-800/80 rounded-xl p-3 flex flex-col items-center shadow-xl">
                <div className="font-mono text-[9px] text-teal-400/50 mb-2 font-bold tracking-widest">
                  // SYSTEM_STATUS
                </div>

                <div className="w-full flex items-center justify-between px-2">
                  {[
                    { label: "ASR", color: "#00FF41", active: true },
                    { label: "LLM", color: "#40C4FF", active: true },
                    { label: "TTS", color: "#E8A838", active: true },
                    { label: "MLLM", color: "#AA77FF", active: false },
                    { label: "VAG", color: "#FF5577", active: false },
                  ].map((node) => (
                    <div key={node.label} className="flex flex-col items-center">
                      {/* 3D Glossy Sphere with Upper-Left Specular Highlight */}
                      <div
                        className="relative rounded-full flex items-center justify-center transition-all shadow-md"
                        style={{
                          width: `${config.statusNodeSizeDp * 0.65}px`,
                          height: `${config.statusNodeSizeDp * 0.65}px`,
                          background: node.active
                            ? `radial-gradient(circle at 35% 35%, ${node.color}, ${node.color}99 60%, #000000 100%)`
                            : `radial-gradient(circle at 35% 35%, ${node.color}44, ${node.color}11 80%, #000)`,
                          boxShadow: node.active ? `0 0 12px ${node.color}66` : "none",
                        }}
                      >
                        {/* Upper-Left Specular Highlight Glint */}
                        {node.active && (
                          <div className="absolute top-[18%] left-[20%] w-[32%] h-[32%] rounded-full bg-white/70 blur-[0.5px]" />
                        )}
                      </div>
                      <span
                        className={`font-mono text-[10px] font-bold mt-1.5 tracking-tight ${
                          node.active ? "text-slate-200" : "text-slate-600"
                        }`}
                        style={{ color: node.active ? node.color : undefined }}
                      >
                        {node.label}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Right Column: Controls & Parameter Tweaker */}
      <div className="lg:col-span-5 flex flex-col gap-4">
        {/* Controls Card Header */}
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 shadow-md">
          <div className="flex items-center justify-between border-b border-slate-800 pb-3 mb-4">
            <div className="flex items-center gap-2">
              <Sliders className="w-4 h-4 text-teal-400" />
              <h2 className="font-mono font-bold text-sm text-slate-100">
                Visual Track Controls (V.1–V.11)
              </h2>
            </div>
            <button
              onClick={resetConfig}
              className="text-xs font-mono text-slate-400 hover:text-teal-300 flex items-center gap-1 transition-colors"
            >
              <RotateCcw className="w-3 h-3" />
              Reset
            </button>
          </div>

          {/* Tab Switcher for Parameter Categories */}
          <div className="flex items-center gap-2 mb-4 bg-slate-950 p-1 rounded-lg border border-slate-800">
            <button
              onClick={() => setActiveTab("controls")}
              className={`flex-1 text-xs font-mono py-1.5 rounded transition-colors ${
                activeTab === "controls" ? "bg-teal-500/20 text-teal-300 font-semibold" : "text-slate-400 hover:text-slate-200"
              }`}
            >
              Layout & Scaling
            </button>
            <button
              onClick={() => setActiveTab("kotlin")}
              className={`flex-1 text-xs font-mono py-1.5 rounded transition-colors ${
                activeTab === "kotlin" ? "bg-teal-500/20 text-teal-300 font-semibold" : "text-slate-400 hover:text-slate-200"
              }`}
            >
              Kotlin Output
            </button>
          </div>

          {activeTab === "kotlin" ? (
            <div className="space-y-3 font-mono text-xs">
              <div className="text-slate-400 text-xs flex items-center justify-between">
                <span>Generated Jetpack Compose parameters:</span>
                <span className="text-teal-400">HomeGrid.kt</span>
              </div>
              <pre className="bg-slate-950 p-3 rounded-lg border border-slate-800 text-teal-300 overflow-x-auto text-[11px] leading-relaxed max-h-96">
{`// HomeGrid.kt Visual Track Configurations
val tileW = ${config.cardWidthDp}.dp // V.2 Card width
val tileH = ${config.cardHeightDp}.dp // V.2 Card height
val iconSize = ${config.iconSizeDp}.dp // V.3 Protruding icon size
val titleFontSize = ${config.titleFontSizeSp}.sp // V.2 Title font scale
val logoText = "${config.logoText}" // V.10 Logo string
val logoFontSize = ${config.logoFontSizeSp}.sp // V.10 Logo font scale

// Hub & Crystal
val crystalScale = ${config.crystalScale}f // V.5 Crystal 3D gem scale
val whiteSunGlow = ${config.crystalWhiteSunGlow} // V.5 White sun aura
val bgDarkness = Color(${config.backgroundDarkness.replace("#", "0xFF")}) // V.1 Astral background

// Status Spheres
val statusNodeSize = ${config.statusNodeSizeDp}.dp // V.7 3D glossy spheres`}
              </pre>
            </div>
          ) : (
            <div className="space-y-4 max-h-[520px] overflow-y-auto pr-1">
              {/* V.2 & V.4 Tile Cards & Proportions */}
              <div className="bg-slate-950 p-3 rounded-lg border border-slate-800 space-y-3">
                <div className="text-xs font-mono font-bold text-teal-400 flex items-center gap-1.5">
                  <span className="px-1.5 py-0.5 rounded bg-teal-500/20 text-[10px]">V.2 & V.4</span>
                  Tile Cards & Font Scaling
                </div>

                <div>
                  <div className="flex justify-between text-xs text-slate-300 font-mono mb-1">
                    <span>Card Dimensions:</span>
                    <span className="text-teal-400">{config.cardWidthDp}dp × {config.cardHeightDp}dp</span>
                  </div>
                  <input
                    type="range"
                    min="108"
                    max="160"
                    value={config.cardWidthDp}
                    onChange={(e) => {
                      const val = parseInt(e.target.value);
                      setConfig((c) => ({ ...c, cardWidthDp: val, cardHeightDp: Math.round(val * 1.21) }));
                    }}
                    className="w-full accent-teal-400"
                  />
                  <p className="text-[10px] text-slate-400 mt-0.5">
                    Original 108×130dp was too small (names truncated). Target is ~132×160dp.
                  </p>
                </div>

                <div>
                  <div className="flex justify-between text-xs text-slate-300 font-mono mb-1">
                    <span>Protruding Icon Size:</span>
                    <span className="text-teal-400">{config.iconSizeDp}dp</span>
                  </div>
                  <input
                    type="range"
                    min="36"
                    max="60"
                    value={config.iconSizeDp}
                    onChange={(e) => setConfig((c) => ({ ...c, iconSizeDp: parseInt(e.target.value) }))}
                    className="w-full accent-teal-400"
                  />
                </div>
              </div>

              {/* V.5 Center Hub Router Crystal */}
              <div className="bg-slate-950 p-3 rounded-lg border border-slate-800 space-y-3">
                <div className="text-xs font-mono font-bold text-teal-400 flex items-center gap-1.5">
                  <span className="px-1.5 py-0.5 rounded bg-teal-500/20 text-[10px]">V.5</span>
                  3D Hexagonal Router Crystal
                </div>

                <div>
                  <div className="flex justify-between text-xs text-slate-300 font-mono mb-1">
                    <span>Crystal Scale:</span>
                    <span className="text-teal-400">{config.crystalScale.toFixed(2)}x</span>
                  </div>
                  <input
                    type="range"
                    min="0.8"
                    max="1.5"
                    step="0.05"
                    value={config.crystalScale}
                    onChange={(e) => setConfig((c) => ({ ...c, crystalScale: parseFloat(e.target.value) }))}
                    className="w-full accent-teal-400"
                  />
                </div>

                <div className="flex items-center justify-between text-xs text-slate-300 font-mono">
                  <span>White Sun Radial Aura:</span>
                  <input
                    type="checkbox"
                    checked={config.crystalWhiteSunGlow}
                    onChange={(e) => setConfig((c) => ({ ...c, crystalWhiteSunGlow: e.target.checked }))}
                    className="accent-teal-400 w-4 h-4 rounded"
                  />
                </div>
              </div>

              {/* V.7 System Status Nodes */}
              <div className="bg-slate-950 p-3 rounded-lg border border-slate-800 space-y-3">
                <div className="text-xs font-mono font-bold text-teal-400 flex items-center gap-1.5">
                  <span className="px-1.5 py-0.5 rounded bg-teal-500/20 text-[10px]">V.7</span>
                  3D Glossy Status Spheres
                </div>

                <div>
                  <div className="flex justify-between text-xs text-slate-300 font-mono mb-1">
                    <span>Sphere Sphere Diameter:</span>
                    <span className="text-teal-400">{config.statusNodeSizeDp}dp</span>
                  </div>
                  <input
                    type="range"
                    min="36"
                    max="64"
                    value={config.statusNodeSizeDp}
                    onChange={(e) => setConfig((c) => ({ ...c, statusNodeSizeDp: parseInt(e.target.value) }))}
                    className="w-full accent-teal-400"
                  />
                </div>
              </div>

              {/* V.6 Curved Plasma Cords */}
              <div className="bg-slate-950 p-3 rounded-lg border border-slate-800 space-y-3">
                <div className="text-xs font-mono font-bold text-teal-400 flex items-center gap-1.5">
                  <span className="px-1.5 py-0.5 rounded bg-teal-500/20 text-[10px]">V.6</span>
                  Curved Plasma Cords
                </div>

                <div>
                  <div className="flex justify-between text-xs text-slate-300 font-mono mb-1">
                    <span>Cord Curvature:</span>
                    <span className="text-teal-400">{(config.plasmaTubeCurve * 100).toFixed(0)}%</span>
                  </div>
                  <input
                    type="range"
                    min="0.05"
                    max="0.5"
                    step="0.01"
                    value={config.plasmaTubeCurve}
                    onChange={(e) => setConfig((c) => ({ ...c, plasmaTubeCurve: parseFloat(e.target.value) }))}
                    className="w-full accent-teal-400"
                  />
                </div>

                <div>
                  <div className="flex justify-between text-xs text-slate-300 font-mono mb-1">
                    <span>Tube Thickness:</span>
                    <span className="text-teal-400">{config.plasmaTubeThickness}px</span>
                  </div>
                  <input
                    type="range"
                    min="1.5"
                    max="6.0"
                    step="0.5"
                    value={config.plasmaTubeThickness}
                    onChange={(e) => setConfig((c) => ({ ...c, plasmaTubeThickness: parseFloat(e.target.value) }))}
                    className="w-full accent-teal-400"
                  />
                </div>

                <div>
                  <div className="flex justify-between text-xs text-slate-300 font-mono mb-1">
                    <span>Energy Beads Count:</span>
                    <span className="text-teal-400">{config.plasmaTubeBeadsCount} beads</span>
                  </div>
                  <input
                    type="range"
                    min="2"
                    max="12"
                    value={config.plasmaTubeBeadsCount}
                    onChange={(e) => setConfig((c) => ({ ...c, plasmaTubeBeadsCount: parseInt(e.target.value) }))}
                    className="w-full accent-teal-400"
                  />
                </div>
              </div>

              {/* V.1 Background & Telemetry */}
              <div className="bg-slate-950 p-3 rounded-lg border border-slate-800 space-y-3">
                <div className="text-xs font-mono font-bold text-teal-400 flex items-center gap-1.5">
                  <span className="px-1.5 py-0.5 rounded bg-teal-500/20 text-[10px]">V.1</span>
                  Astral Background & Telemetry
                </div>

                <div>
                  <div className="flex justify-between text-xs text-slate-300 font-mono mb-1">
                    <span>Extra Telemetry Clusters:</span>
                    <span className="text-teal-400">{config.extraTelemetryClusters} clusters</span>
                  </div>
                  <input
                    type="range"
                    min="1"
                    max="4"
                    value={config.extraTelemetryClusters}
                    onChange={(e) => setConfig((c) => ({ ...c, extraTelemetryClusters: parseInt(e.target.value) }))}
                    className="w-full accent-teal-400"
                  />
                </div>

                <div>
                  <div className="flex justify-between text-xs text-slate-300 font-mono mb-1">
                    <span>3D Star Density:</span>
                    <span className="text-teal-400">{config.starsCount} stars</span>
                  </div>
                  <input
                    type="range"
                    min="60"
                    max="240"
                    step="20"
                    value={config.starsCount}
                    onChange={(e) => setConfig((c) => ({ ...c, starsCount: parseInt(e.target.value) }))}
                    className="w-full accent-teal-400"
                  />
                </div>
              </div>
            </div>
          )}

          {/* Action Button: Apply parameters to HomeGrid.kt */}
          <div className="pt-4 border-t border-slate-800">
            <button
              onClick={onApplyToKotlin}
              disabled={isApplying}
              className="w-full py-2.5 px-4 rounded-xl bg-teal-500 hover:bg-teal-400 text-slate-950 font-mono font-bold text-xs flex items-center justify-center gap-2 shadow-lg shadow-teal-500/20 transition-all disabled:opacity-50"
            >
              {isApplying ? (
                <span>Applying changes to HomeGrid.kt...</span>
              ) : appliedSuccess ? (
                <>
                  <Check className="w-4 h-4 text-slate-950" />
                  Applied to HomeGrid.kt!
                </>
              ) : (
                <>
                  <Zap className="w-4 h-4" />
                  Sync Parameters to HomeGrid.kt Source
                </>
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
