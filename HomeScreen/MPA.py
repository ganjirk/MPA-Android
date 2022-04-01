import kivy
import sys
sys.path.insert(0, '../NumGuess/')
from kivymd.app import MDApp
from kivymd.uix.screen import MDScreen
from kivy.uix.gridlayout import GridLayout
from kivy.uix.label import Label
from kivy.uix.image import Image
from kivy.uix.button import Button
from kivy.uix.textinput import TextInput
from kivymd.uix.toolbar import MDToolbar
from NumGuess import NumGuess


class MPA(MDApp):

    def __init__(self, **kwargs): 
        super().__init__(**kwargs)
        # ToolBar
        self.toolbar = MDToolbar(title = "Welcome o MPA!!!")
        self.toolbar.pos_hint = {"top":1 }
        # self.toolbar.right_action_items = [["home", lambda x:self.home()]]
        self.theme_cls.primary_palette = "Gray"
        self.header = Label (text = "Hi", color = 'brown', font_size=20)

        
    def buttons(self):
        #button widget 1
        self.button = Button(text="Number Guess",
                             background_color = 'blue',
                             size_hint = (0.3, 0.15),
                             border = (0, 20, 0, 15),
                             bold = True,
                             pos_hint = {"x":0.1, "y":0.3}
                            )
        self.button.bind(on_press=lambda x:NumGuess(MDApp))
        
        #button widget 2
        self.button = Button(text="Number",
                             background_color = 'blue',
                             size_hint = (0.3, 0.15),
                             border = (0, 20, 0, 15),
                             bold = True,
                             pos_hint = {"x":0.1, "y":0.1}
                            )
        # self.button.bind(on_press=NumGuess())
        screen.add_widget(self.button)
        # return screen
    
    def build(self):

        screen = MDScreen()
        # Add logo  to screen
        screen.add_widget (Image(source="../static/logo.jpg"))
        # Add toolbar to sceen
        screen.add_widget (self.toolbar)
        screen.add_widget (self.header)

        self.button = Button(text="Number Guess",
                             background_color = 'blue',
                             size_hint = (0.3, 0.15),
                             border = (0, 20, 0, 15),
                             bold = True,
                             pos_hint = {"x":0.1, "y":0.3}
                            )
        self.button.bind(on_press=lambda x:NumGuess().run())

        screen.add_widget(self.button)
        return screen  

    def callback(self, instance):
        self.greeting.text = "Hi " + self.user.text 

        
if __name__ == '__main__':
    MPA().run()