#pragma once
#include "mce/textureptr.h"
class ItemInstance;
class ItemGraphics {
public:
	//char filler[16 /*sizeof(mce::TexturePtr)*/]; // 0
	mce::TexturePtr texturePtr;
	ItemGraphics(mce::TexturePtr&& _texturePtr) {
		texturePtr = std::move(_texturePtr);
	}
	ItemGraphics() {
	}
};
class ItemRenderer {
public:
	std::vector<ItemGraphics> itemGraphics; // 0
	void _loadItemGraphics();
	static mce::TexturePtr const& getGraphics(ItemInstance const&);
	static mce::TexturePtr const& getGraphics(Item const&);
	static std::vector<ItemGraphics> mItemGraphics;
	static ItemRenderer instance;
};
static_assert(offsetof(ItemRenderer, itemGraphics) == 0, "itemrenderer offset wrong");