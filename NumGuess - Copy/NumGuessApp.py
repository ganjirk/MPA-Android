import kivy
from kivymd.app import MDApp
from kivy.app import App
from kivymd.uix.screen import MDScreen
from kivy.uix.gridlayout import GridLayout
from kivy.uix.boxlayout import BoxLayout
from kivymd.uix.boxlayout import MDBoxLayout
from kivy.uix.label import Label
from kivymd.uix.label import MDLabel
from kivy.uix.image import Image
from kivy.uix.button import Button
from kivymd.uix.button import MDFillRoundFlatButton, MDFillRoundFlatIconButton
from kivy.uix.textinput import TextInput
from kivymd.uix.toolbar import MDToolbar
from kivymd.theming import ThemeManager
from kivy.lang import Builder
from kivy.uix.widget import Widget
import random


# Builder.load_file("numguess.kv")

class root_layt(MDBoxLayout):
    pass


rand_num = random.randint(1, 50)
val = 0
guesses_left = 5

kv = Builder.load_file('numguess.kv')

class NumGuessApp(MDApp):
    
    guesses_left = 5
    def guess_cnt(self):
        global guesses_left 
        
        guesses_left -=1
        guesses_left_msg = f"Chances Left: {guesses_left}"
        self.root.ids.guesses_left_msg.text = guesses_left_msg  
        

    def reset1(self):
        rand_num = random.randint(1, 50)

    def check(self):    
        global rand_num, val         
        
        try:              
            
            val = self.root.ids.input_num.text  
            val = int(val) 
            print(val, rand_num, guesses_left)
            if val == rand_num:
                guess_msg = f"Number: {val} is Correct Guess"   
                self.root.ids.guess_msg.text = guess_msg
                rand_num = random.randint(1, 50) 
                
            elif val < 1:
                guess_msg = "Enter between 1 to 50"
                self.root.ids.guess_msg.text = guess_msg
            elif val > rand_num:
                guess_msg = "Hint: Enter Lower Number"
                self.root.ids.guess_msg.text = guess_msg                
            
            else:
                guess_msg = "Hint: Enter Higher Number"
                self.root.ids.guess_msg.text = guess_msg                             
            
            
        except:
            guess_msg = "Only Numbers Allowed !!!"
            self.root.ids.guess_msg.text = guess_msg
    


    def build(self):
        return kv
    
       
if __name__ == '__main__':
    NumGuessApp().run()