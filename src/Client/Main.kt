package Client

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage

class Main : Application() {

    @Throws(Exception::class)
    override fun start(primaryStage: Stage) {
        val root = FXMLLoader.load<Parent>(Main::class.java.getResource("MainScene.fxml"))
        primaryStage.title = "Hello World"
        primaryStage.scene = Scene(root, 300.0, 500.0)
        primaryStage.show()
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) {
            Application.launch(Main::class.java)
        }
    }
}
