# Changelog

## Unreleased
- Added chromatic (trichrome) printing process
- Red, Green and Blue filters now have stronger effect to enable chromatic process

Lightroom:
- When in creative mode, you can now hold [Shift] to print exposure instantly and without dyes/paper   
- Experience points, granted for printing an image, are now different per process - bw/color/chromatic. Config now has three options instead of one to configure this. 


---

## 1.4.1 - 2024-03-15
- Fixed Lenses config resetting and not working properly.
- Fixed issue with `Cold Sweat` when closing viewfinder with filter installed causing blur shader to apply when it shouldn't.
- Fixed `FTB Teams`, `REI` and `REI Plugin Compat` causing the game to freeze indefinitely when Album is opened.  


- [Fabric] Fixed Create's Spout crashing with latest `Create Fabric`.
- There will be no crash anymore if the `Create` version is not supported. Spout Film Developing will not work for incompatible versions instead. 
- Create 0.5.1f is needed for Spout Film Developing to work. 

---

## 1.4.0 - 2024-03-02
- Added Aged Photographs. Created by crafting a Photograph with brown dye
- Stacked Photographs max stack size can now be changed in a config


- Lowered required Fabric API version to 0.88.1+
- Updated localization files 


- Fixed incorrect position of a Photograph in Quark's Glass Item Frame
- Fixed crash when opening Album that's placed in a Lectern
- Fixed crash when clicking on Exposure entry in Mods menu

---

## 1.3.1 - 2024-02-09
- Added advancement for taking a selfie
- Slightly changed order of advancements
- Entities in frame (in photograph NBT) will now work correctly for selfies 
- Black and white photograph copying recipe now correctly uses only black dye instead of all four colors
- Changed Film Frame Exposed advancement trigger from `minecraft:frame_exposed` to `exposure:frame_exposed`
- [Fabric] Proper message will now show when `Fabric API` or `Forge Config Api Port` is not installed 
- [Fabric] Fixed crash when signing album

---

## 1.3.0 - 2024-02-04
- Added Photo Album 
  - Can store up to 16 photographs and some notes
  - Can be placed on Chiseled Bookshelves and Lecterns


- Changed photograph paper texture to not be so rough at the edges
- Holding use when opening Viewfinder will no longer cause Camera to shoot immediately after opening
- Camera controls key can be remapped now. Unbound means it will use sneak key, as before. Unbound by default.   


- Fixed non-english characters in player's nickname causing a crash when trying to render their photos 
- Fixed Lightroom not resetting selected frame back to 0 when film is ejected and another is inserted
- Fixed Lightroom not dropping due to the missing loot-table
- [Forge] Fixed item frame also rendering an item over the photograph 

---

## 1.2.2 - 2024-01-01
- Added `/exposure export` command. Allows exporting exposures to PNGs to `<world>/exposures` folder. Requires OP privileges.
- Added some **creative-mode** tools to Photograph screen:
  - **Ctrl+S** to save as PNG
  - **Ctrl+C** to copy exposure id to clipboard
  - **Ctrl+P** to give yourself current photograph in item form

- Fixed being able to copy `Copy of a copy` photograph.
- Fixed fov related issue that was causing problems with zoom mods.
- Fixed Lightroom Screen film bugging out when replacing Developed Film in slot with a film that has fewer frames than currently selected frame index.   
- Made some changes that may fix the crash with C2ME.

---

## 1.2.1 - 2023-12-24
- Fixed water not rendering properly with shaders when looking through Viewfinder.

---

## 1.2.0 - 2023-12-22
- Added `exposure:flashes` and `exposure:lenses` tags, allowing customization of items that can be attached to the camera. 
- Added Focal Length config options, allowing to configure default camera range and range per specific lens.

---

#### Changed Developing Recipe:
- Developing recipes will no longer show in vanilla Recipe Book due to the book not working well with this type of recipes (like with cloning written books or dyeing armor)   
- Developing can now be done with Create's Spouts. Configurable.
- Films no longer can be developed with Create's Mechanical Crafter

---

#### Misc: 
- Added zh_cn localization provided by 'IwasConfused'
- When JEI is not present, tooltips will be shown describing Developing and Photograph Copying recipes.
- Items can now be extracted from any side of a Lightroom block. And inserted through all but bottom side.
- Fixed Camera Attachments menu not opening from offhand.
- Pressing Inventory Key or Esc in thirdperson-back camera mode (when viewfinder is not visible) will now deactivate camera instead of opening inventory or pause menu.
- Slightly reduced z-fighting of the Hanging Photograph at greater distances.

---

## 1.1.1 - 2023-12-08
- Fixed third-person camera distance being closer when not looking through the Viewfinder.

---

## 1.1.0 - 2023-12-07
- Added selfies. Press F5 (by default) to be the star of the show. 

- Fixed Viewfinder Controls not showing up when sneak is bound to one of the mouse buttons.
- Fixed errors in log about developing and cloning recipes

---

## 1.0.2 - 2023-12-03
- Fixed Hanging Photograph not dropping when the block it's attached to is broken.
- Fixed crash when trying to add more than 16 photographs to the Stacked Photographs item.

---

## 1.0.1 - 2023-12-01
- Film developing recipe has been removed from Create's Automated Shapeless Crafting due to Mixer's whisk being too rough on the film, clearing any exposed images.   
